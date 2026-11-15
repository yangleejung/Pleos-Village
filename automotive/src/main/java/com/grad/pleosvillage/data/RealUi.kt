package com.grad.pleosvillage.data

import com.grad.pleosvillage.domain.EngineState
import com.grad.pleosvillage.domain.Env
import com.grad.pleosvillage.domain.ResidentTable
import com.grad.pleosvillage.domain.StopState
import com.grad.pleosvillage.domain.StopSummary
import com.grad.pleosvillage.ui.art.MockVillageObjects
import com.grad.pleosvillage.ui.art.ObjType
import com.grad.pleosvillage.ui.art.ResidentKind
import com.grad.pleosvillage.ui.model.ArrivalState
import com.grad.pleosvillage.ui.model.ArrivalUi
import com.grad.pleosvillage.ui.model.DayScore
import com.grad.pleosvillage.ui.model.Deduction
import com.grad.pleosvillage.ui.model.DexUi
import com.grad.pleosvillage.ui.model.RecordsUi
import com.grad.pleosvillage.ui.model.ResidentStatus
import com.grad.pleosvillage.ui.model.ResidentUi
import com.grad.pleosvillage.ui.model.TripUi
import com.grad.pleosvillage.ui.model.VillageUi
import com.grad.pleosvillage.ui.model.WeekStat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Judging engine state (EngineState) → screen models. Screens stay as-is; only the data becomes real (stage 4).
 * Environment sentences and names reuse FakeRepository's resident definitions (SPEC 4.3 wording).
 */
object RealUi {
    private val templates = FakeRepository.dex.residents.associateBy { it.id }

    private fun fmt(pattern: String, wall: Long) = SimpleDateFormat(pattern, Locale.KOREA).format(Date(wall))
    private fun dateShort(wall: Long) = fmt("M월 d일", wall)

    fun duration(ms: Long): String {
        val min = (ms / 60_000).toInt()
        return if (min >= 60) "${min / 60}시간 ${min % 60}분" else "${min}분"
    }

    fun envName(e: Env) = when (e) {
        Env.RAIN -> "비"
        Env.FAMILY -> "가족 동승"
        Env.NIGHT -> "밤"
        Env.WINTER -> "성에 제거"
        Env.LONG_TRIP -> "장거리"
        Env.CHARGE -> "충전"
        Env.LOCATION -> "위치"
    }

    /** A single resident (with progress applied). */
    fun resident(s: EngineState, id: String): ResidentUi {
        val t = templates.getValue(id)
        val rule = ResidentTable.byId(id)
        val p = s.residents[id]
        if (!rule.judged && rule.env == Env.LOCATION) return t
        return t.copy(
            status = if (p?.met == true) ResidentStatus.Met else ResidentStatus.NotYet,
            missed = p?.missed ?: 0,
            metDate = p?.metAtWall?.let { dateShort(it) },
            metStory = p?.metAtWall?.let { "${dateShort(it)}에 만났어요.\n그날 안전 점수 ${p.metScore ?: 100}점." },
        )
    }

    fun dex(s: EngineState) = DexUi(FakeRepository.dex.residents.map { resident(s, it.id) })

    private fun stateOf(x: StopState) = when (x) {
        StopState.RESULTS -> ArrivalState.Results
        StopState.BELOW_PASS -> ArrivalState.BelowPass
        StopState.SHORT_TRIP -> ArrivalState.ShortTrip
        StopState.DAILY_LIMIT -> ArrivalState.DailyLimit
        StopState.NO_ENVIRONMENT -> ArrivalState.NoEnvironment
        StopState.LATER_STOP -> ArrivalState.LaterStop
    }

    /** Arrival summary. */
    fun arrival(s: EngineState, x: StopSummary): ArrivalUi {
        val deductions = buildList {
            if (x.harshSteer > 0) add(Deduction("급한 핸들 조작", x.harshSteer, -10 * x.harshSteer))
            if (x.harshBrake > 0) add(Deduction("급제동", x.harshBrake, -10 * x.harshBrake))
        }
        return ArrivalUi(
            id = x.id.toString(),
            state = stateOf(x.state),
            score = x.score,
            deductions = deductions,
            met = x.met.map { resident(s, it).copy(status = ResidentStatus.Met) },
            missed = x.missed.map { (id, left) ->
                val rule = ResidentTable.byId(id)
                resident(s, id).copy(status = ResidentStatus.NotYet, missed = rule.ceiling - left)
            },
            arrivedAt = fmt("M월 d일 EEEE a h:mm", x.atWall),
            dateShort = dateShort(x.atWall),
            duration = duration(x.drivingMs),
            environments = x.envs.map(::envName),
            passengers = x.passengers,
            points = x.points,
            chancesTotal = x.chancesTotal,
            chancesLeft = x.chancesLeft,
            hint = x.hint?.let { resident(s, it) },
            // This emulator/vehicle can't read the seatbelt warning (CLAUDE.md "에뮬레이터 제한").
            unchecked = listOf("안전벨트"),
            drivingMinutes = (x.drivingMs / 60_000).toInt(),
            rests = x.rests,
        )
    }

    fun arrivalById(s: EngineState, id: String): ArrivalUi? =
        id.toLongOrNull()?.let { n -> s.summaries.firstOrNull { it.id == n } }?.let { arrival(s, it) }

    private val kindOf = mapOf(
        ResidentKind.Rain to "rain", ResidentKind.Night to "night", ResidentKind.Family to "family", ResidentKind.Winter to "winter",
    )

    /** Village: only met residents walk around. Object placement and points come from the saved decorate state. */
    fun village(s: EngineState, decor: DecorState): VillageUi {
        val base = FakeRepository.village(decor)
        val metKinds = kindOf.filter { (_, id) -> s.residents[id]?.met == true }.keys
        fun keep(o: com.grad.pleosvillage.ui.art.VillageObject) = o.type != ObjType.Resident || o.kind in metKinds
        val residentsAt = MockVillageObjects.filter { it.type == ObjType.Resident && it.kind in metKinds }
            .associate { (it.i to it.j) to resident(s, kindOf.getValue(it.kind!!)) }
        return base.copy(
            subtitle = "${fmt("M월", System.currentTimeMillis())}의 마을 · 주민 ${residentsAt.size}명",
            chancesLeft = s.chancesLeft(AppRepository.today()),
            spec = base.spec.copy(extras = base.spec.extras.filter(::keep), bubbles = emptyList()),
            specNarrow = base.specNarrow.copy(extras = base.specNarrow.extras.filter(::keep), bubbles = emptyList()),
            residentsAt = residentsAt,
            weeklyAverage = records(s).stats.firstOrNull()?.value?.toIntOrNull() ?: 0,
            weeklyDelta = 0,
        )
    }

    /** Records: computed from this week's (Mon–Sun) stop summaries. */
    fun records(s: EngineState): RecordsUi {
        val cal = Calendar.getInstance(Locale.KOREA).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis > System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, -7)
        val weekStart = cal.timeInMillis
        val weekEnd = weekStart + 7L * 24 * 3600 * 1000
        val week = s.summaries.filter { it.atWall in weekStart until weekEnd }
        val trips = week.map { x ->
            val wd = Calendar.getInstance().apply { timeInMillis = x.atWall }.get(Calendar.DAY_OF_WEEK).let { (it + 5) % 7 }
            TripUi(x.id.toString(), "${dateShort(x.atWall)} (${fmt("E", x.atWall)})", fmt("a h:mm", x.atWall), wd, arrival(s, x))
        }
        val counted = trips.filter { it.counted }
        val avg = if (counted.isEmpty()) 0 else counted.map { it.score }.average().toInt()
        val pass = counted.count { it.score >= 80 }
        val harsh = counted.sumOf { it.harshCount }
        val long = counted.filter { it.summary.drivingMinutes > 120 }
        val restPct = if (long.isEmpty()) 0 else long.count { it.summary.rests > 0 } * 100 / long.size
        val none = "지난주 기록 없음"
        return RecordsUi(
            weekRange = "${dateShort(weekStart)} ~ ${dateShort(weekEnd - 1)}",
            stats = listOf(
                WeekStat("평균 안전 점수", "$avg", "점", none, true),
                WeekStat("80점 이상 운행", "$pass/${counted.size}", "번", none, true),
                WeekStat("급한 핸들 조작·급제동", "$harsh", "번", none, harsh == 0),
                WeekStat("2시간 넘는 운행에서 쉬어 간 비율", "$restPct", "%", none, true),
            ),
            days = listOf("월", "화", "수", "목", "금", "토", "일").mapIndexed { i, label ->
                val sc = counted.filter { it.weekday == i }.map { it.score }
                DayScore(label, if (sc.isEmpty()) null else sc.average().toInt())
            },
            trips = trips,
        )
    }
}
