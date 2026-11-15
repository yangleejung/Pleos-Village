package com.grad.pleosvillage.data

import com.grad.pleosvillage.ui.art.MockVillageSpec
import com.grad.pleosvillage.ui.art.MockVillageSpecNarrow
import com.grad.pleosvillage.ui.art.MockVillageObjects
import com.grad.pleosvillage.ui.art.ObjType
import com.grad.pleosvillage.ui.art.ResidentKind
import com.grad.pleosvillage.ui.model.ArrivalState
import com.grad.pleosvillage.ui.model.ArrivalUi
import com.grad.pleosvillage.ui.model.Deduction
import com.grad.pleosvillage.ui.model.DexUi
import com.grad.pleosvillage.ui.model.ResidentStatus
import com.grad.pleosvillage.ui.model.ResidentUi
import com.grad.pleosvillage.ui.model.VillageUi
import com.grad.pleosvillage.ui.model.CheckUi
import com.grad.pleosvillage.ui.model.RecordsUi
import com.grad.pleosvillage.ui.model.SettingsUi
import com.grad.pleosvillage.ui.model.TripUi
import com.grad.pleosvillage.ui.model.WeekStat
import com.grad.pleosvillage.ui.model.DayScore

/**
 * Mockup data. Used to build screens without the domain layer and check them in previews/emulator.
 * Replaced by the real Repository in stage 4. Content matches docs/design/mockups.html.
 */
object FakeRepository {

    val rain = ResidentUi(
        "rain", ResidentKind.Rain, ResidentKind.Rain.residentName, "와이퍼가 동작한 운행", "비 오는 날",
        40, 3, 0, ResidentStatus.Met, "11월 7일",
        "11월 7일, 비 오는 퇴근길에 만났어요.\n그날 안전 점수 100점.",
    )
    val night = ResidentUi(
        "night", ResidentKind.Night, ResidentKind.Night.residentName, "하향등을 켜고 달린 운행", "밤길",
        30, 4, 0, ResidentStatus.Met, "11월 6일",
        "11월 6일, 밤길을 달려 만났어요.\n그날 안전 점수 95점.",
    )
    val family = ResidentUi(
        "family", ResidentKind.Family, ResidentKind.Family.residentName, "뒷좌석에 사람이 탄 운행", "가족과 함께",
        50, 2, 0, ResidentStatus.Met, "11월 9일",
        "11월 9일, 가족과 함께 만났어요.\n그날 안전 점수 100점.",
    )
    val winter = ResidentUi(
        "winter", ResidentKind.Winter, ResidentKind.Winter.residentName, "성에 제거를 쓴 운행", "추운 아침",
        30, 4, 0, ResidentStatus.Met, "11월 10일",
        "11월 10일, 추운 아침에 만났어요.\n그날 안전 점수 90점.",
    )
    val longTrip = ResidentUi(
        "longtrip", ResidentKind.LongTrip, ResidentKind.LongTrip.residentName, "2시간 넘는 운행에서 쉰 운행", "긴 운행",
        50, 2, 1, ResidentStatus.NotYet,
    )
    val charge = ResidentUi(
        "charge", ResidentKind.Charge, ResidentKind.Charge.residentName, "충전한 날 (전기차)", "충전한 날",
        30, 4, 2, ResidentStatus.NotYet,
    )
    val sea = ResidentUi(
        "sea", ResidentKind.Sea, ResidentKind.Sea.residentName, "바닷가 도로를 달린 운행", "바다",
        null, null, 0, ResidentStatus.ComingSoon,
    )
    val mountain = ResidentUi(
        "mountain", ResidentKind.Mountain, ResidentKind.Mountain.residentName, "산길을 달린 운행", "산",
        null, null, 0, ResidentStatus.ComingSoon,
    )

    val dex = DexUi(listOf(rain, night, family, winter, longTrip, charge, sea, mountain))

    /** Resident positions on the village (mockup VOBJ). */
    private val residentsAt = mapOf((2 to 2) to rain, (4 to 2) to night, (2 to 5) to winter, (5 to 6) to family)
    val residentTiles: Set<Pair<Int, Int>> get() = residentsAt.keys

    /** Builds the village from a decorate state. Item placement and points come from [DecorStore]. */
    fun village(decor: DecorState): VillageUi {
        val residents = MockVillageObjects.filter { it.type == ObjType.Resident }
        val extras = residents + DecorStore.objects(decor)
        return VillageUi(
            title = "우리 마을",
            subtitle = "11월의 마을 · 주민 ${residentsAt.size}명",
            points = decor.points,
            chancesTotal = 2,
            chancesLeft = 1,
            weeklyAverage = 94,
            weeklyDelta = 8,
            spec = MockVillageSpec.copy(map = DecorStore.TERRAIN, extras = extras),
            specNarrow = MockVillageSpecNarrow.copy(map = DecorStore.TERRAIN, extras = extras),
            residentsAt = residentsAt,
        )
    }

    /** The village with the currently saved decorate state. */
    val village: VillageUi get() = village(DecorStore.saved.value)

    private val familyMissed = family.copy(status = ResidentStatus.NotYet, missed = 1, metDate = null)

    /** The 6 arrival summary states (UI_SPEC 4.1 table). Selected by id. */
    val arrivals: Map<String, ArrivalUi> = listOf(
        ArrivalUi(
            "met", ArrivalState.Results, 100, emptyList(), listOf(rain), listOf(familyMissed),
            "11월 14일 토요일 오후 1:58", "11월 14일", "1시간 50분", listOf("비", "가족 동승"), 4, 20, 2, 1,
            unchecked = listOf("안전벨트"), drivingMinutes = 110,
        ),
        ArrivalUi(
            "below", ArrivalState.BelowPass, 70,
            listOf(Deduction("급한 핸들 조작", 2, -20), Deduction("급제동", 1, -10)), emptyList(), emptyList(),
            "11월 14일 토요일 오후 6:20", "11월 14일", "1시간 5분", listOf("비"), 2, 0, 2, 2,
            unchecked = listOf("안전벨트"), drivingMinutes = 65,
        ),
        ArrivalUi(
            "short", ArrivalState.ShortTrip, 100, emptyList(), emptyList(), emptyList(),
            "11월 14일 토요일 오전 9:12", "11월 14일", "7분", emptyList(), 1, 0, 2, 2, drivingMinutes = 7,
        ),
        ArrivalUi(
            "limit", ArrivalState.DailyLimit, 90, listOf(Deduction("급한 핸들 조작", 1, -10)), emptyList(), emptyList(),
            "11월 14일 토요일 오후 8:40", "11월 14일", "52분", listOf("밤"), 1, 0, 2, 0, drivingMinutes = 52,
        ),
        ArrivalUi(
            "none", ArrivalState.NoEnvironment, 100, emptyList(), emptyList(), emptyList(),
            "11월 14일 토요일 오전 11:05", "11월 14일", "40분", emptyList(), 1, 20, 2, 1, hint = longTrip, drivingMinutes = 40,
        ),
        ArrivalUi(
            "later", ArrivalState.LaterStop, 90, listOf(Deduction("급제동", 1, -10)), listOf(family), emptyList(),
            "11월 14일 토요일 오후 3:30", "11월 14일", "2시간 30분", listOf("가족 동승"), 4, 0, 2, 1, drivingMinutes = 150, rests = 1,
        ),
    ).associateBy { it.id }

    /* ---------------- Records (UI_SPEC 4.6): 10 trips this week ---------------- */

    private val weekExtra = listOf(
        ArrivalUi(
            "mon", ArrivalState.Results, 100, emptyList(), listOf(family), emptyList(),
            "11월 9일 월요일 오전 10:20", "11월 9일", "1시간 10분", listOf("가족 동승"), 4, 20, 2, 1, drivingMinutes = 70,
        ),
        ArrivalUi(
            "tue", ArrivalState.Results, 90, listOf(Deduction("급한 핸들 조작", 1, -10)), listOf(winter), emptyList(),
            "11월 10일 화요일 오전 7:45", "11월 10일", "35분", listOf("성에 제거"), 1, 20, 2, 1, drivingMinutes = 35,
        ),
        ArrivalUi(
            "wed", ArrivalState.NoEnvironment, 80, listOf(Deduction("급한 핸들 조작", 1, -10), Deduction("급제동", 1, -10)),
            emptyList(), emptyList(), "11월 11일 수요일 오후 7:30", "11월 11일", "50분", emptyList(), 1, 10, 2, 1,
            hint = longTrip, drivingMinutes = 50,
        ),
        ArrivalUi(
            "thu", ArrivalState.NoEnvironment, 100, emptyList(), emptyList(), emptyList(),
            "11월 12일 목요일 오후 5:10", "11월 12일", "25분", emptyList(), 2, 20, 2, 1, hint = charge, drivingMinutes = 25,
        ),
    )

    private val weekdays = listOf("월", "화", "수", "목", "금", "토", "일")

    /** "11월 14일 토요일 오후 1:58" → one row of the trip list. */
    private fun trip(a: ArrivalUi): TripUi {
        val parts = a.arrivedAt.split(' ')
        val wd = weekdays.indexOf(parts[2].take(1))
        return TripUi(a.id, "${parts[0]} ${parts[1]} (${weekdays[wd]})", "${parts[3]} ${parts[4]}", wd, a)
    }

    /** Most recent trips first (within a day, time order follows the mockup data order). */
    val trips: List<TripUi> = listOf(
        arrivals.getValue("limit"), arrivals.getValue("below"), arrivals.getValue("later"), arrivals.getValue("met"),
        arrivals.getValue("none"), arrivals.getValue("short"),
    ).map(::trip) + weekExtra.reversed().map(::trip)

    /** Trip id → arrival summary (trip detail). */
    fun tripSummary(id: String): ArrivalUi? = trips.firstOrNull { it.id == id }?.summary

    /** Last week's values (mockup data for comparison) */
    private const val LAST_AVG = 86
    private const val LAST_PASS_PCT = 71
    private const val LAST_HARSH = 11
    private const val LAST_REST_PCT = 0

    val records: RecordsUi = run {
        val counted = trips.filter { it.counted }
        val avg = counted.map { it.score }.average().toInt()
        val pass = counted.count { it.score >= 80 }
        val passPct = pass * 100 / counted.size
        val harsh = counted.sumOf { it.harshCount }
        val long = counted.filter { it.summary.drivingMinutes > 120 }
        val rested = long.count { it.summary.rests > 0 }
        val restPct = if (long.isEmpty()) 0 else rested * 100 / long.size
        fun diff(now: Int, last: Int, unit: String) = when {
            now > last -> "지난주보다 ${now - last}$unit 올랐어요"
            now < last -> "지난주보다 ${last - now}$unit 내려갔어요"
            else -> "지난주와 같아요"
        }
        RecordsUi(
            weekRange = "11월 9일 ~ 11월 15일",
            stats = listOf(
                WeekStat("평균 안전 점수", "$avg", "점", diff(avg, LAST_AVG, "점"), avg >= LAST_AVG),
                WeekStat("80점 이상 운행", "$pass/${counted.size}", "번", diff(passPct, LAST_PASS_PCT, "%p"), passPct >= LAST_PASS_PCT),
                WeekStat(
                    "급한 핸들 조작·급제동", "$harsh", "번",
                    if (harsh <= LAST_HARSH) "지난주보다 ${LAST_HARSH - harsh}번 줄었어요" else "지난주보다 ${harsh - LAST_HARSH}번 늘었어요",
                    harsh <= LAST_HARSH,
                ),
                WeekStat("2시간 넘는 운행에서 쉬어 간 비율", "$restPct", "%", diff(restPct, LAST_REST_PCT, "%p"), restPct >= LAST_REST_PCT),
            ),
            days = weekdays.mapIndexed { i, label ->
                val s = counted.filter { it.weekday == i }.map { it.score }
                DayScore(label, if (s.isEmpty()) null else s.average().toInt())
            },
            trips = trips,
        )
    }

    /* ---------------- Settings (UI_SPEC 4.7) ---------------- */

    /** Support status confirmed on emulator v2.1.0 (CLAUDE.md "에뮬레이터 제한"). */
    val settings = SettingsUi(
        checks = listOf(
            CheckUi("와이퍼", true, ResidentKind.Rain.residentName),
            CheckUi("뒷좌석 탑승", true, ResidentKind.Family.residentName),
            CheckUi("하향등", true, ResidentKind.Night.residentName),
            CheckUi("성에 제거", true, ResidentKind.Winter.residentName),
            CheckUi("조향각", true, "급한 핸들 조작"),
            CheckUi("브레이크", true, "급제동"),
            CheckUi("충전", true, ResidentKind.Charge.residentName),
            CheckUi("안전벨트", false, "안전벨트 위반"),
        ),
        version = "1.0",
    )
}
