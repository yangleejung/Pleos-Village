#!/usr/bin/env bash
# Pleos Village automated demo scenario runner (docs/SPEC.md §10).
#
# Usage (Git Bash, from the project root):
#   tools/scenarios.sh            # all of A B C D
#   tools/scenarios.sh A B        # only the chosen ones
#
# Prerequisites:
#   - The emulator is running and the debug build is installed (gradlew :automotive:installDebug).
#   - The Virtual Console connection is on (opening http://127.0.0.1:8088/ in a browser turns it on).
#     If bridge_ready in /vehicle/state is false, writes are rejected with 503 (emulator_inactive).
#   - python is on PATH (used to check the result JSON).
#
# Time scale 60× (1 real second = 1 vehicle minute). Results are checked from the app's save file (files/village_state.json),
# and screenshots are kept in build/scenarios/. Exit code 1 if any scenario fails.

set -u
export PYTHONIOENCODING=utf-8
cd "$(dirname "$0")/.."

SDK_DIR=$(grep '^sdk.dir' local.properties | cut -d= -f2- | sed 's/\\\\/\//g; s/\\:/:/g; s/\\//g')
ADB="${ADB:-$SDK_DIR/platform-tools/adb.exe}"
[ -x "$ADB" ] || ADB=adb
PKG=com.grad.pleosvillage
V=http://127.0.0.1:8088
OUT=build/scenarios
mkdir -p "$OUT"
FAILED=0

log()  { echo "[$(date +%T)] $*"; }
wait_s() { "$ADB" shell sleep "$1"; }   # wait on the device instead of a local sleep

put() {
  local code
  code=$(curl -s -m 10 -o /dev/null -w '%{http_code}' -X PUT "$V$1")
  if [ "$code" != "200" ]; then log "PUT $1 -> $code (Virtual Console 연결을 확인하세요)"; return 1; fi
}

app() { "$ADB" shell am start -n "$PKG/.MainActivity" "$@" >/dev/null 2>&1; }

state_json() { "$ADB" shell run-as "$PKG" --user 10 cat files/village_state.json 2>/dev/null || "$ADB" shell run-as "$PKG" cat files/village_state.json; }

# Check: evaluates a python expression against the latest arrival summary s and resident states r (id→dict).
check() {
  local name="$1" expr="$2"
  if state_json | python -c "
import json,sys
d=json.load(sys.stdin); s=d['summaries'][0] if d['summaries'] else {}; r=d['residents']
ok=eval(sys.argv[1])
print('   요약:', {k:s.get(k) for k in ('state','score','harshSteer','harshBrake','met','missed','points','drivingMs','rests')})
sys.exit(0 if ok else 1)" "$expr"; then
    log "✔ $name 통과"
  else
    log "✘ $name 실패 (조건: $expr)"; FAILED=1
  fi
  "$ADB" exec-out screencap -p > "$OUT/$name.png"
}

preflight() {
  "$ADB" forward tcp:8088 tcp:8088 >/dev/null
  if ! curl -s -m 10 "$V/vehicle/state" | grep -q '"bridge_ready":true'; then
    log "Virtual Console 연결이 꺼져 있습니다 (bridge_ready=false). 브라우저에서 $V/ 를 열어 켠 뒤 다시 실행하세요."; exit 2
  fi
}

# Start state for every scenario: P, wiper/defroster off, steering 0, brake 0, driver + rear seat occupied, demo data
setup() {
  put "/vehicle/gear?gear=Park"
  put "/vehicle/body?front_wiper=Off"
  put "/vehicle/hvac?front_defroster=false&rear_defroster=false"
  put "/vehicle/controls?steering=0"
  put "/vehicle/controls?brake=0"
  app --es route village --es demo "reset,scale60,filteroff,demo"
  wait_s 4
  put "/vehicle/seat?kind=occupied&area=driver&value=true"
  put "/vehicle/seat?kind=occupied&area=rear_left&value=true"
  wait_s 2
}

scenario_A() {
  log "A. 레이니(비)와 해삐(가족): 와이퍼 켜고 16분 주행 → 100점, 두 주민, +20"
  setup
  put "/vehicle/gear?gear=Drive"; wait_s 2
  put "/vehicle/body?front_wiper=Low"; wait_s 16
  put "/vehicle/gear?gear=Park"; wait_s 4
  check A "s['state']=='RESULTS' and s['score']==100 and {'rain','family'} <= set(s['met']) and s['points']==20"
}

scenario_B() {
  log "B. 안전 점수 미달: 급한 핸들 조작 2번 + 급제동 1번 → 70점, 주민 없음, 못 만난 횟수 그대로"
  setup
  put "/vehicle/gear?gear=Drive"; wait_s 2
  put "/vehicle/body?front_wiper=Low"; wait_s 3
  put "/vehicle/controls?steering=200"; wait_s 4     # #1 (wait out the 3 s cooldown)
  put "/vehicle/controls?steering=0"; wait_s 2        # #2
  put "/vehicle/controls?brake=80"; wait_s 1; put "/vehicle/controls?brake=0"
  wait_s 8
  put "/vehicle/gear?gear=Park"; wait_s 4
  check B "s['state']=='BELOW_PASS' and s['score']==70 and not s['met'] and r['rain']['missed']==2 and r['family']['missed']==1"
}

scenario_C() {
  log "C. 저니(장거리): D 110분 → P 15분(휴식) → D 20분 → P  (약 2분 30초 걸림)"
  setup
  put "/vehicle/gear?gear=Drive"; wait_s 110
  put "/vehicle/gear?gear=Park"; wait_s 15
  put "/vehicle/gear?gear=Drive"; wait_s 20
  put "/vehicle/gear?gear=Park"; wait_s 4
  check C "'longtrip' in s['met'] and s['rests']>=1 and s['drivingMs']>=120*60000"
}

scenario_D() {
  log "D. 하루 한도: 판정 운행 2번을 채운 뒤 A를 한 번 더 → DAILY_LIMIT, 판정하지 않음"
  setup
  for i in 1 2; do
    put "/vehicle/gear?gear=Drive"; wait_s 12
    put "/vehicle/gear?gear=Park"; wait_s 2
    app --es demo "endtrip"; wait_s 1
  done
  put "/vehicle/gear?gear=Drive"; wait_s 2
  put "/vehicle/body?front_wiper=Low"; wait_s 16
  put "/vehicle/gear?gear=Park"; wait_s 4
  check D "s['state']=='DAILY_LIMIT' and not s['met'] and r['rain']['missed']==2"
}

preflight
LIST=("$@"); [ ${#LIST[@]} -eq 0 ] && LIST=(A B C D)
for sc in "${LIST[@]}"; do
  case "$sc" in A|B|C|D) "scenario_$sc" ;; *) log "모르는 시나리오: $sc (A B C D)"; FAILED=1 ;; esac
done
# when done, restore P and wiper off
put "/vehicle/gear?gear=Park"; put "/vehicle/body?front_wiper=Off"
[ $FAILED -eq 0 ] && log "모두 통과. 캡처: $OUT/" || log "실패한 시나리오가 있습니다. 캡처: $OUT/"
exit $FAILED
