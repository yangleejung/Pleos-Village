package com.grad.pleosvillage.data

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.grad.pleosvillage.domain.Engine
import com.grad.pleosvillage.domain.EngineState
import com.grad.pleosvillage.domain.Env
import com.grad.pleosvillage.domain.RandomSource
import com.grad.pleosvillage.domain.ResidentProgress
import com.grad.pleosvillage.domain.ResidentTable
import com.grad.pleosvillage.domain.Settings
import com.grad.pleosvillage.domain.StopState
import com.grad.pleosvillage.domain.StopSummary
import com.grad.pleosvillage.domain.TimeSource
import com.grad.pleosvillage.domain.TripState
import com.grad.pleosvillage.vehicle.GearPosition
import com.grad.pleosvillage.vehicle.Reading
import com.grad.pleosvillage.vehicle.VehicleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/*
 * Real data repository. Data flow: VehicleDataSource → Engine (judging) → this repository → screens (StateFlow). SPEC §7.
 * Saved to a single JSON file in app-internal storage (no location is stored). Saved on every change and every 30 s while driving.
 * This object is shared by the service and the screens within the same process.
 */
object AppRepository {
    private const val TAG = "PleosVillage"
    private const val FILE = "village_state.json"
    private const val SAVE_EVERY_MS = 30_000L

    private val time = object : TimeSource {
        override fun nowMs() = SystemClock.elapsedRealtime()
        override fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        override fun wallMs() = System.currentTimeMillis()
    }
    private val random = RandomSource { Random.nextInt(100) }

    private var file: File? = null
    private lateinit var engine: Engine
    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var feedJob: Job? = null
    private var last = VehicleState()

    /** Environments this vehicle doesn't support (based on diagnostics). */
    @Volatile private var unsupported: Set<Env> = emptySet()

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, FILE)
        file = f
        val loaded = runCatching { if (f.exists()) Json.decode(JSONObject(f.readText())) else null }
            .onFailure { Log.w(TAG, "저장 파일 읽기 실패 (새로 시작)", it) }.getOrNull()
        // On start, keep any in-progress trip, but clear the park time since a process restart may have reset the monotonic clock.
        engine = Engine(loaded?.copy(parkedAtMs = null) ?: EngineState(), time, random) { unsupported }
        publish(save = false)
        Log.i(TAG, "[repo] 불러옴: 만난 주민 ${engine.state.residents.count { it.value.met }}, 요약 ${engine.state.summaries.size}")
    }

    /** Feeds vehicle values received by the service into the judging engine. */
    fun attach(vehicle: Flow<VehicleState>) {
        feedJob?.cancel()
        feedJob = scope.launch {
            launch { vehicle.collect { feed(it) } }
            launch {
                while (isActive) {
                    delay(SAVE_EVERY_MS)
                    if (engine.state.trip != null) { engine.tick(); publish() }
                }
            }
        }
    }

    private fun <T> Reading<T>.v(): T? = (this as? Reading.Value<T>)?.value

    private fun feed(s: VehicleState) {
        if (!::engine.isInitialized) return
        unsupported = buildSet {
            if (s.wiperSupport is Reading.Unsupported) add(Env.RAIN)
            if (s.seatOccupancySupport is Reading.Unsupported) add(Env.FAMILY)
            if (s.lowBeam is Reading.Unsupported) add(Env.NIGHT)
            if (s.defrosterSupport is Reading.Unsupported) add(Env.WINTER)
        }
        var changed = false
        // Order: environment values first, gear last (so judging on P uses fully settled values)
        val wiper = s.wiper.values.any { it.v() == "ON" }
        if (wiper != last.wiper.values.any { it.v() == "ON" }) { engine.onWiper(wiper); changed = true }
        if (s.lowBeam.v() != last.lowBeam.v()) { engine.onLowBeam(s.lowBeam.v() == "ON"); changed = true }
        val defrost = s.defroster.values.any { it.v() == true }
        if (defrost != last.defroster.values.any { it.v() == true }) { engine.onDefrost(defrost); changed = true }
        if (s.seatOccupancy != last.seatOccupancy) {
            val occupied = s.seatOccupancy.filter { it.value.v() == true }.keys
            val rear = occupied.any { it.contains("ROW_2") || it.contains("ROW_3") }
            engine.onSeats(rear, occupied.size.coerceAtLeast(1)); changed = true
        }
        val angle = s.steeringAngle.v()
        if (angle != null && angle != last.steeringAngle.v()) { engine.onSteering(angle); changed = true }
        val brake = s.brakePressed.v()
        if (brake != null && brake != last.brakePressed.v()) { engine.onBrake(brake); changed = true }
        val gear = s.gear.v()
        if (gear != null && gear != last.gear.v()) {
            val summary = engine.onGear(driving = gear == GearPosition.DRIVE || gear == GearPosition.REVERSE, parked = gear == GearPosition.PARK)
            summary?.let { Log.i(TAG, "[judge] 정차: 상태=${it.state} 점수=${it.score} 만남=${it.met} 못만남=${it.missed} 포인트=+${it.points} 주행=${it.drivingMs / 60000}분 환경=${it.envs}") }
            changed = true
        }
        last = s
        if (changed) publish()
    }

    private fun publish(save: Boolean = true) {
        _state.value = engine.state
        if (save) file?.let { f -> runCatching { f.writeText(Json.encode(engine.state).toString()) }.onFailure { Log.w(TAG, "저장 실패", it) } }
    }

    /* ---------------- Called from screens and the debug menu ---------------- */

    fun markSeen(id: Long) { engine.markSeen(id); publish() }
    fun setTimeScale(scale: Int) { engine.setSettings(engine.state.settings.copy(timeScale = scale)); publish() }
    fun setSpeedFilter(on: Boolean) { engine.setSettings(engine.state.settings.copy(speedFilter = on)); publish() }
    fun resetAll() { engine.resetAll(); publish(); Log.i(TAG, "[repo] 데이터 초기화") }
    fun resetToday() { engine.resetToday(); publish() }
    fun endTrip() { engine.endTrip(); publish() }
    fun loadDemo() { engine.loadDemo(); publish(); Log.i(TAG, "[repo] 시연 데이터(천장 직전) 불러옴") }
    fun today() = time.today()
}

/** EngineState ↔ JSON. */
private object Json {
    fun encode(s: EngineState) = JSONObject().apply {
        put("settings", JSONObject().put("timeScale", s.settings.timeScale).put("speedFilter", s.settings.speedFilter))
        put("residents", JSONObject().apply {
            s.residents.forEach { (id, p) ->
                put(id, JSONObject().put("met", p.met).put("metAtWall", p.metAtWall ?: JSONObject.NULL).put("metScore", p.metScore ?: JSONObject.NULL).put("missed", p.missed))
            }
        })
        put("judgeCount", JSONObject().apply { s.judgeCount.forEach { (d, c) -> put(d, c) } })
        put("points", s.points)
        put("trip", s.trip?.let { encodeTrip(it) } ?: JSONObject.NULL)
        put("summaries", JSONArray().apply { s.summaries.forEach { put(encodeSummary(it)) } })
        put("unseen", s.unseen ?: JSONObject.NULL)
        put("nextId", s.nextId)
    }

    private fun encodeTrip(t: TripState) = JSONObject().put("id", t.id).put("startWall", t.startWall).put("drivingMs", t.drivingMs)
        .put("rests", t.rests).put("wiperMs", t.wiperMs).put("rearMs", t.rearMs).put("lowBeamMs", t.lowBeamMs).put("defrostMs", t.defrostMs)
        .put("harshSteer", t.harshSteer).put("harshBrake", t.harshBrake).put("judging", t.judging)
        .put("judged", JSONArray(t.judged.toList())).put("maxPassengers", t.maxPassengers)

    private fun encodeSummary(x: StopSummary) = JSONObject().put("id", x.id).put("tripId", x.tripId).put("atWall", x.atWall)
        .put("state", x.state.name).put("score", x.score).put("harshSteer", x.harshSteer).put("harshBrake", x.harshBrake)
        .put("drivingMs", x.drivingMs).put("rests", x.rests).put("passengers", x.passengers)
        .put("envs", JSONArray(x.envs.map { it.name })).put("met", JSONArray(x.met))
        .put("missed", JSONArray(x.missed.map { JSONObject().put("id", it.first).put("left", it.second) }))
        .put("points", x.points).put("chancesTotal", x.chancesTotal).put("chancesLeft", x.chancesLeft).put("hint", x.hint ?: JSONObject.NULL)

    fun decode(o: JSONObject): EngineState {
        val st = o.getJSONObject("settings")
        val res = o.getJSONObject("residents")
        val residents = ResidentTable.all.associate { r ->
            val p = res.optJSONObject(r.id)
            r.id to if (p == null) ResidentProgress() else ResidentProgress(
                met = p.optBoolean("met"),
                metAtWall = if (p.isNull("metAtWall")) null else p.getLong("metAtWall"),
                metScore = if (p.isNull("metScore")) null else p.getInt("metScore"),
                missed = p.optInt("missed"),
            )
        }
        val jc = o.getJSONObject("judgeCount")
        val trip = if (o.isNull("trip")) null else o.getJSONObject("trip").let { t ->
            TripState(
                id = t.getLong("id"), startWall = t.getLong("startWall"), drivingMs = t.getLong("drivingMs"), rests = t.getInt("rests"),
                wiperMs = t.getLong("wiperMs"), rearMs = t.getLong("rearMs"), lowBeamMs = t.getLong("lowBeamMs"), defrostMs = t.getLong("defrostMs"),
                harshSteer = t.getInt("harshSteer"), harshBrake = t.getInt("harshBrake"), judging = t.getBoolean("judging"),
                judged = t.getJSONArray("judged").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() },
                maxPassengers = t.optInt("maxPassengers", 1),
            )
        }
        val sums = o.getJSONArray("summaries").let { a ->
            (0 until a.length()).map { i ->
                val x = a.getJSONObject(i)
                StopSummary(
                    id = x.getLong("id"), tripId = x.getLong("tripId"), atWall = x.getLong("atWall"), state = StopState.valueOf(x.getString("state")),
                    score = x.getInt("score"), harshSteer = x.getInt("harshSteer"), harshBrake = x.getInt("harshBrake"), drivingMs = x.getLong("drivingMs"),
                    rests = x.getInt("rests"), passengers = x.getInt("passengers"),
                    envs = x.getJSONArray("envs").let { e -> (0 until e.length()).map { Env.valueOf(e.getString(it)) } },
                    met = x.getJSONArray("met").let { e -> (0 until e.length()).map { e.getString(it) } },
                    missed = x.getJSONArray("missed").let { e -> (0 until e.length()).map { e.getJSONObject(it).let { m -> m.getString("id") to m.getInt("left") } } },
                    points = x.getInt("points"), chancesTotal = x.getInt("chancesTotal"), chancesLeft = x.getInt("chancesLeft"),
                    hint = if (x.isNull("hint")) null else x.getString("hint"),
                )
            }
        }
        return EngineState(
            settings = Settings(st.optInt("timeScale", 1), st.optBoolean("speedFilter", true)),
            residents = residents,
            judgeCount = jc.keys().asSequence().associateWith { jc.getInt(it) },
            points = o.optInt("points"),
            trip = trip,
            summaries = sums,
            unseen = if (o.isNull("unseen")) null else o.getLong("unseen"),
            nextId = o.optLong("nextId", 1),
        )
    }
}
