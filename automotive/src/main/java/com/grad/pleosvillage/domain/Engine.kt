package com.grad.pleosvillage.domain

import kotlin.math.max

/*
 * Judging engine. SPEC §3 (terms), §4 (score/judging), §5 (points), §6 (flow).
 * Call on…() whenever a vehicle value changes; the moment the gear becomes P it builds a [StopSummary].
 * The state (EngineState) is an immutable value, so saving/restoring is easy. The data package does the saving.
 */

data class Settings(
    /** Time scale. 1 in a real car, 60 for the demo (1 real second = 1 minute). */
    val timeScale: Int = 1,
    /** Speed filter. When on, harsh maneuvers below 20 km/h are not counted. The emulator can't change speed, so it is off for the demo. */
    val speedFilter: Boolean = true,
)

/** Resident progress. */
data class ResidentProgress(
    val met: Boolean = false,
    /** Date met (wall clock ms) */
    val metAtWall: Long? = null,
    /** Safety score of the trip in which it was met */
    val metScore: Int? = null,
    /** Number of misses so far, m */
    val missed: Int = 0,
)

/** Accumulated values of the trip in progress (vehicle time ms). */
data class TripState(
    val id: Long,
    val startWall: Long,
    val drivingMs: Long = 0,
    val rests: Int = 0,
    val wiperMs: Long = 0,
    val rearMs: Long = 0,
    val lowBeamMs: Long = 0,
    val defrostMs: Long = 0,
    val harshSteer: Int = 0,
    val harshBrake: Int = 0,
    /** Whether it has been chosen as a judging trip (SPEC 4.2, item 2) */
    val judging: Boolean = false,
    /** Residents already judged in this trip */
    val judged: Set<String> = emptySet(),
    /** Maximum occupants during this trip (including the driver) */
    val maxPassengers: Int = 1,
)

/** Arrival summary state. Same meaning as the screen's ArrivalState. */
enum class StopState { RESULTS, BELOW_PASS, SHORT_TRIP, DAILY_LIMIT, NO_ENVIRONMENT, LATER_STOP }

/** Arrival summary built on stop (SPEC 6.2, item 4). */
data class StopSummary(
    val id: Long,
    val tripId: Long,
    val atWall: Long,
    val state: StopState,
    val score: Int,
    val harshSteer: Int,
    val harshBrake: Int,
    val drivingMs: Long,
    val rests: Int,
    val passengers: Int,
    /** Environments fulfilled in this trip */
    val envs: List<Env>,
    val met: List<String>,
    /** Missed resident id → judgings left until the ceiling */
    val missed: List<Pair<String, Int>>,
    val points: Int,
    val chancesTotal: Int,
    val chancesLeft: Int,
    /** Resident to show as a hint when no environment was fulfilled */
    val hint: String? = null,
)

data class EngineState(
    val settings: Settings = Settings(),
    val residents: Map<String, ResidentProgress> = ResidentTable.all.associate { it.id to ResidentProgress() },
    /** Date → number of judging trips that day */
    val judgeCount: Map<String, Int> = emptyMap(),
    val points: Int = 0,
    val trip: TripState? = null,
    /** Last time the gear became P (real monotonic ms). Used on next departure to detect rest / trip end. */
    val parkedAtMs: Long? = null,
    /** Recent arrival summaries (newest first, up to 50) */
    val summaries: List<StopSummary> = emptyList(),
    /** Id of the arrival summary not yet seen */
    val unseen: Long? = null,
    val nextId: Long = 1,
) {
    fun chancesLeft(today: String) = (Rules.DAILY_JUDGE_LIMIT - (judgeCount[today] ?: 0)).coerceAtLeast(0)
}

/** Instantaneous values (not saved). Accumulated time is settled whenever a value changes. */
private data class Live(
    val driving: Boolean = false,
    val parked: Boolean = true,
    val wiper: Boolean = false,
    val rear: Boolean = false,
    val lowBeam: Boolean = false,
    val defrost: Boolean = false,
    val passengers: Int = 1,
    val brake: Boolean = false,
    val speedKmh: Float? = null,
)

class Engine(
    initial: EngineState,
    private val time: TimeSource,
    private val random: RandomSource,
    /** Residents that need unsupported data (SPEC 4.2, item 7). Not judged. */
    private val unsupported: () -> Set<Env> = { emptySet() },
) {
    var state: EngineState = initial
        private set

    private var live = Live()
    private var lastSettleMs = time.nowMs()
    /** Steering angle samples (real ms, angle). Only the latest HARSH_WINDOW is kept. */
    private val steer = ArrayDeque<Pair<Long, Float>>()
    private var lastHarshSteerMs = Long.MIN_VALUE / 2

    /** Converts real elapsed time to vehicle time and accumulates it. Call right before a value changes. */
    private fun settle() {
        val now = time.nowMs()
        val dt = (now - lastSettleMs).coerceAtLeast(0) * state.settings.timeScale
        lastSettleMs = now
        val t = state.trip ?: return
        if (!live.driving || dt == 0L) return
        state = state.copy(
            trip = t.copy(
                drivingMs = t.drivingMs + dt,
                wiperMs = t.wiperMs + if (live.wiper) dt else 0,
                rearMs = t.rearMs + if (live.rear) dt else 0,
                lowBeamMs = t.lowBeamMs + if (live.lowBeam) dt else 0,
                defrostMs = t.defrostMs + if (live.defrost) dt else 0,
            ),
        )
    }

    /** Called periodically to bring accumulation up to date (when saving every 30 s while driving). */
    fun tick() = settle()

    fun setSettings(s: Settings) { settle(); state = state.copy(settings = s) }

    /**
     * Gear. driving = D or R, parked = P. N is neither (not counted as driving time, and no stop handling).
     * @return the arrival summary built on entering P (null if none)
     */
    fun onGear(driving: Boolean, parked: Boolean): StopSummary? {
        settle()
        val wasParked = live.parked
        live = live.copy(driving = driving, parked = parked)
        if (wasParked && !parked) startOrResumeTrip()
        if (!wasParked && parked) return stop()
        return null
    }

    fun onWiper(on: Boolean) { settle(); live = live.copy(wiper = on) }
    fun onLowBeam(on: Boolean) { settle(); live = live.copy(lowBeam = on) }
    fun onDefrost(on: Boolean) { settle(); live = live.copy(defrost = on) }
    fun onSpeed(kmh: Float?) { live = live.copy(speedKmh = kmh) }

    /** Seat occupancy. rear = at least one rear seat (ROW_2/ROW_3) occupied, passengers = occupant count (incl. driver). */
    fun onSeats(rear: Boolean, passengers: Int) {
        settle()
        live = live.copy(rear = rear, passengers = max(1, passengers))
        state.trip?.let { t -> if (live.passengers > t.maxPassengers) state = state.copy(trip = t.copy(maxPassengers = live.passengers)) }
    }

    private fun filtered(): Boolean {
        if (!state.settings.speedFilter) return false
        val v = live.speedKmh ?: return false // don't filter if the speed is unknown
        return v < Rules.SPEED_FILTER_KMH
    }

    /**
     * Steering angle (real time). While driving, a change of more than HARSH_ANGLE (90°) within HARSH_WINDOW (1 s) is harsh steering.
     * The angle 1 s ago is taken as "the last value received at that time" (values arrive only on change).
     */
    fun onSteering(angle: Float) {
        val now = time.nowMs()
        val before = steer.lastOrNull { it.first <= now - Rules.HARSH_WINDOW_MS }?.second
        steer.addLast(now to angle)
        while (steer.size > 2 && steer[1].first <= now - Rules.HARSH_WINDOW_MS) steer.removeFirst()
        if (!live.driving || filtered()) return
        if (now - lastHarshSteerMs < Rules.HARSH_COOLDOWN_MS) return
        val window = steer.filter { it.first > now - Rules.HARSH_WINDOW_MS }.map { it.second } + listOfNotNull(before)
        if (window.size < 2) return
        if (window.max() - window.min() > Rules.HARSH_ANGLE) {
            lastHarshSteerMs = now
            state.trip?.let { t -> state = state.copy(trip = t.copy(harshSteer = t.harshSteer + 1)) }
        }
    }

    /**
     * Brake pedal. The SDK only reports pressed or not (no pedal %). A press while driving counts as harsh braking.
     * With the speed filter on, presses below 20 km/h are not counted. (decided 2026-11-14, SPEC §3 note)
     */
    fun onBrake(pressed: Boolean) {
        val wasPressed = live.brake
        live = live.copy(brake = pressed)
        if (!pressed || wasPressed || !live.driving || filtered()) return
        state.trip?.let { t -> state = state.copy(trip = t.copy(harshBrake = t.harshBrake + 1)) }
    }

    private fun startOrResumeTrip() {
        val now = time.nowMs()
        val t = state.trip
        val parkedFor = state.parkedAtMs?.let { (now - it).coerceAtLeast(0) * state.settings.timeScale }
        when {
            t == null || (parkedFor != null && parkedFor > Rules.TRIP_END_GAP) -> {
                state = state.copy(
                    trip = TripState(id = state.nextId, startWall = time.wallMs(), maxPassengers = live.passengers),
                    nextId = state.nextId + 1,
                )
            }
            parkedFor != null && parkedFor > Rules.REST_MIN -> state = state.copy(trip = t.copy(rests = t.rests + 1))
        }
    }

    /** Environments fulfilled in the trip. */
    private fun envsOf(t: TripState): List<Env> {
        val out = mutableListOf<Env>()
        val rule = { id: String -> ResidentTable.byId(id) }
        if (t.wiperMs >= rule("rain").envMinMs) out += Env.RAIN
        if (t.rearMs >= rule("family").envMinMs) out += Env.FAMILY
        if (t.lowBeamMs >= rule("night").envMinMs) out += Env.NIGHT
        if (t.defrostMs >= rule("winter").envMinMs) out += Env.WINTER
        if (t.drivingMs >= Rules.LONG_TRIP_MIN && t.rests >= 1) out += Env.LONG_TRIP
        return out
    }

    /** Stop handling. SPEC 6.2. */
    private fun stop(): StopSummary? {
        val now = time.nowMs()
        val today = time.today()
        var t = state.trip ?: return null
        var st = state
        val score = Rules.score(t.harshSteer, t.harshBrake)
        val envs = envsOf(t)
        val wasJudging = t.judging
        var points = 0
        var stopState: StopState? = null

        // 2–3. Decide whether this is a judging trip
        if (!t.judging) {
            when {
                t.drivingMs < Rules.MIN_TRIP_DRIVING -> stopState = StopState.SHORT_TRIP
                score < Rules.SCORE_PASS -> stopState = StopState.BELOW_PASS
                (st.judgeCount[today] ?: 0) >= Rules.DAILY_JUDGE_LIMIT -> stopState = StopState.DAILY_LIMIT
                else -> {
                    t = t.copy(judging = true)
                    points = Rules.pointsFor(score)
                    st = st.copy(judgeCount = st.judgeCount + (today to (st.judgeCount[today] ?: 0) + 1), points = st.points + points)
                }
            }
        }

        // 4–6. If it is a judging trip, judge residents whose environment was newly fulfilled
        val met = mutableListOf<String>()
        val missed = mutableListOf<Pair<String, Int>>()
        if (t.judging) {
            if (score < Rules.SCORE_PASS) {
                stopState = StopState.BELOW_PASS
            } else {
                val off = unsupported()
                var residents = st.residents
                var judged = t.judged
                for (rule in ResidentTable.all) {
                    if (!rule.judged || rule.env in off || rule.env !in envs) continue
                    val p = residents[rule.id] ?: ResidentProgress()
                    if (p.met || rule.id in judged) continue
                    judged = judged + rule.id
                    val forced = p.missed >= rule.ceiling - 1
                    if (forced || random.nextPercent() < rule.probabilityPct) {
                        residents = residents + (rule.id to p.copy(met = true, metAtWall = time.wallMs(), metScore = score))
                        met += rule.id
                    } else {
                        val np = p.copy(missed = p.missed + 1)
                        residents = residents + (rule.id to np)
                        missed += rule.id to (rule.ceiling - np.missed)
                    }
                }
                t = t.copy(judged = judged)
                st = st.copy(residents = residents)
                stopState = when {
                    met.isNotEmpty() || missed.isNotEmpty() -> if (wasJudging) StopState.LATER_STOP else StopState.RESULTS
                    else -> StopState.NO_ENVIRONMENT
                }
            }
        }

        val hint = if (stopState == StopState.NO_ENVIRONMENT) {
            ResidentTable.all.firstOrNull { r -> r.judged && r.env !in unsupported() && st.residents[r.id]?.met != true }?.id
        } else null
        val summary = StopSummary(
            id = st.nextId, tripId = t.id, atWall = time.wallMs(), state = stopState ?: StopState.NO_ENVIRONMENT,
            score = score, harshSteer = t.harshSteer, harshBrake = t.harshBrake, drivingMs = t.drivingMs, rests = t.rests,
            passengers = max(t.maxPassengers, live.passengers), envs = envs, met = met, missed = missed, points = points,
            chancesTotal = Rules.DAILY_JUDGE_LIMIT, chancesLeft = st.chancesLeft(today), hint = hint,
        )
        state = st.copy(
            trip = t,
            parkedAtMs = now,
            summaries = (listOf(summary) + st.summaries).take(50),
            unseen = summary.id,
            nextId = st.nextId + 1,
        )
        return summary
    }

    fun markSeen(id: Long) { if (state.unseen == id) state = state.copy(unseen = null) }

    /* ---------------- Debug menu (SPEC §8) ---------------- */

    /** Reset data (settings are kept). Steering samples are the car's current values, so they are kept (clearing them would lose the reference angle for the next maneuver). */
    fun resetAll() { state = EngineState(settings = state.settings) }

    /** Reset today's judging count. */
    fun resetToday() { state = state.copy(judgeCount = state.judgeCount - time.today()) }

    /** End the trip (the next departure starts a new trip). */
    fun endTrip() { settle(); state = state.copy(trip = null) }

    /** Demo data: state just before the ceiling (레이니 missed 2x, 해삐 1x, 저니 1x). SPEC §10. */
    fun loadDemo() {
        val r = state.residents.toMutableMap()
        r["rain"] = ResidentProgress(missed = 2)
        r["family"] = ResidentProgress(missed = 1)
        r["longtrip"] = ResidentProgress(missed = 1)
        state = state.copy(residents = r)
    }
}

