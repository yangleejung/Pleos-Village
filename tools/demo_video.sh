#!/usr/bin/env bash
# Pleos Village automated demo video recording (for submission).
# Flow: first-run onboarding → empty village → driving (idle screen) → stop, meet residents → village → dex → decorate.
#
# Usage (Git Bash, from the project root):
#   tools/demo_video.sh
# Output: build/demo/pleos_village_demo.mp4 (per-scene originals in build/demo/scenes/)
#
# Prerequisites:
#   - The emulator (wide layout, 1 app window) is running and the debug build is installed.
#   - The Virtual Console connection is on (open http://127.0.0.1:8088/ in a browser).
#   - ffmpeg (winget install Gyan.FFmpeg).
#   - Don't touch the emulator while recording.
# Screen coordinates assume a 2560×1440 emulator + wide layout. Update the TAP_* values if the layout changes.

set -u
export MSYS_NO_PATHCONV=1          # keep Git Bash from rewriting /sdcard paths
cd "$(dirname "$0")/.."

SDK_DIR=$(grep '^sdk.dir' local.properties | cut -d= -f2- | sed 's/\\\\/\//g; s/\\:/:/g; s/\\//g')
ADB="${ADB:-$SDK_DIR/platform-tools/adb.exe}"; [ -x "$ADB" ] || ADB=adb
FF="${FFMPEG:-$(command -v ffmpeg || echo "$LOCALAPPDATA/Microsoft/WinGet/Links/ffmpeg.exe")}"
PKG=com.grad.pleosvillage
V=http://127.0.0.1:8088
OUT=build/demo; SCN="$OUT/scenes"; mkdir -p "$SCN"; rm -f "$SCN"/*.mp4

# ---- Screen coordinates (px) ----
TAP_ONB_NEXT="1693 1020"       # onboarding [다음]/[시작하기]
TAP_GO_VILLAGE="2321 1201"     # arrival summary [마을로 데려가기]
TAP_NAV_DEX="960 590"          # village screen left menu '도감'
TAP_NAV_VILLAGE="960 421"      # dex screen left menu '마을'
TAP_DEX_FIRST="1261 499"       # first dex card (레이니)
TAP_DECORATE="2349 1201"       # village [마을 꾸미기]
TAP_TAB_DECO="1187 1002"       # decorate drawer '장식' tab
TAP_ITEM_FIRST="1005 1126"     # first drawer item (fountain)
TAP_TILE="2112 602"            # target tile
TAP_PLACE="1722 878"           # [여기 놓기]
TAP_DONE="2410 177"            # [완료]

log() { echo "[$(date +%T)] $*"; }
# With no touch for 10+ s on a main tab, the idle screen appears. Tap an empty spot (the sky) once to wake it before pressing a menu.
wake() { "$ADB" shell input tap 1700 230; "$ADB" shell sleep 1; }
w() { "$ADB" shell sleep "$1"; }
tap() { "$ADB" shell input tap $1; }
put() { local c; c=$(curl -s -m 10 -o /dev/null -w '%{http_code}' -X PUT "$V$1"); [ "$c" = 200 ] || log "PUT $1 -> $c"; }
app() { "$ADB" shell "am start -n $PKG/.MainActivity $*" >/dev/null 2>&1; }
# Caption (don't use single quotes). Leading ^ = top, ~ = above the decorate drawer, none = bottom.
cap() { app "--es caption '$1'"; }

REC_PID=""
rec_start() {   # $1 = scene number
  "$ADB" shell screenrecord --size 1920x1080 --bit-rate 12000000 "/sdcard/demo_$1.mp4" &
  REC_PID=$!; w 1
}
rec_stop() {
  w 1
  "$ADB" shell "kill -2 \$(pidof screenrecord)" 2>/dev/null
  wait "$REC_PID" 2>/dev/null; w 1
  "$ADB" pull "/sdcard/demo_$1.mp4" "$SCN/$1.mp4" >/dev/null 2>&1 && "$ADB" shell rm "/sdcard/demo_$1.mp4"
  log "장면 $1 저장"
}

# ---- Setup ----
"$ADB" forward tcp:8088 tcp:8088 >/dev/null
if ! curl -s -m 10 "$V/vehicle/state" | grep -q '"bridge_ready":true'; then
  log "Virtual Console 연결이 꺼져 있습니다. 브라우저에서 $V/ 를 연 뒤 다시 실행하세요."; exit 2
fi
[ -x "$FF" ] || { log "ffmpeg가 없습니다 (winget install Gyan.FFmpeg)"; exit 2; }
log "준비: 처음 실행 상태 (데이터·꾸미기 초기화, 60배속, 천장 직전 시연 데이터)"
put "/vehicle/gear?gear=Park"; put "/vehicle/body?front_wiper=Off"
put "/vehicle/hvac?front_defroster=false&rear_defroster=false"
put "/vehicle/controls?steering=0"; put "/vehicle/controls?brake=0"
put "/vehicle/seat?kind=occupied&area=rear_left&value=false"
app "--es route village --es demo reset,decorreset,scale60,filteroff,demo,naviOff --es caption ''"
w 3

# ---- 1. First run: onboarding ----
rec_start 1
app "--es demo onboarding"; w 2
cap "처음 실행하면 짧은 안내가 나와요"; w 4
tap "$TAP_ONB_NEXT"; w 4
tap "$TAP_ONB_NEXT"; cap "안전 점수 80점 이상이면 그날의 환경에 맞는 주민을 만나요"; w 4
tap "$TAP_ONB_NEXT"; w 2
cap "아직 아무도 살지 않는 우리 마을"; w 4
rec_stop 1

# ---- 2. Driving: only the idle screen while driving ----
rec_start 2
cap "가족과 함께 출발! 뒷좌석에 사람이 탔어요"
put "/vehicle/seat?kind=occupied&area=driver&value=true"; put "/vehicle/seat?kind=occupied&area=rear_left&value=true"; w 3
put "/vehicle/gear?gear=Drive"; w 1
cap "기어 D: 운전 중에는 시계와 마을만 보이고, 조작할 수 없어요"; w 5
put "/vehicle/body?front_wiper=Low"
cap "비가 와서 와이퍼를 켰어요 (시연은 1초가 1분)"; w 7
cap "운전하는 동안 핸들, 브레이크, 와이퍼, 좌석을 기록해요"; w 6
put "/vehicle/body?front_wiper=Off"; w 1
rec_stop 2

# ---- 3. Stop: meet residents in the arrival summary ----
rec_start 3
cap ""
put "/vehicle/gear?gear=Park"; w 4
w 6        # show the arrival summary without captions (so they don't cover the title and score)
tap "$TAP_GO_VILLAGE"; w 2
cap "안전 점수 100점! 비 오는 날 레이니, 가족과 함께라 해삐를 만났어요"; w 4
cap "만난 주민은 마을에서 살아요"; w 4
rec_stop 3

# ---- 4. Dex ----
rec_start 4
wake; tap "$TAP_NAV_DEX"; w 1
cap "도감에 만난 주민이 모여요"; w 4
cap "주민을 누르면 큰 카드로 볼 수 있어요"; w 2
wake; tap "$TAP_DEX_FIRST"; w 4
"$ADB" shell input keyevent KEYCODE_BACK; w 1
cap ""; w 1
rec_stop 4

# ---- 5. Decorate ----
rec_start 5
wake; tap "$TAP_NAV_VILLAGE"; w 2
wake; tap "$TAP_DECORATE"; w 2
cap "~안전 운전 포인트로 마을을 꾸며요"; w 3
tap "$TAP_TAB_DECO"; w 1
tap "$TAP_ITEM_FIRST"; w 1
tap "$TAP_TILE"; w 1
cap "~분수를 고르고 놓을 칸을 누르면 미리 보여 줘요"; w 4
tap "$TAP_PLACE"; w 2
tap "$TAP_DONE"; w 2
cap "완성! 다음 운전도 안전하게, Pleos Village"; w 5
cap ""; w 1
rec_stop 5

# ---- Concatenate ----
LIST="$OUT/scenes.txt"; : > "$LIST"
for i in 1 2 3 4 5; do [ -f "$SCN/$i.mp4" ] && echo "file 'scenes/$i.mp4'" >> "$LIST"; done
"$FF" -y -loglevel error -f concat -safe 0 -i "$LIST" -c:v libx264 -crf 20 -preset medium -pix_fmt yuv420p -r 30 -an "$OUT/pleos_village_demo.mp4" \
  && log "완성: $OUT/pleos_village_demo.mp4 ($("$FF" -i "$OUT/pleos_village_demo.mp4" 2>&1 | grep -o 'Duration: [0-9:.]*'))" \
  || { log "합치기 실패"; exit 1; }
put "/vehicle/gear?gear=Park"
