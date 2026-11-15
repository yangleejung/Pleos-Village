package com.grad.pleosvillage.ui.model

import androidx.compose.runtime.Immutable
import com.grad.pleosvillage.ui.art.ResidentKind
import com.grad.pleosvillage.ui.art.VillageSpec

/*
 * Models used by the screens. Kept separate from the domain (judging logic) so the real Repository can build them in stage 4.
 */

enum class ResidentStatus { Met, NotYet, ComingSoon }

@Immutable
data class ResidentUi(
    val id: String,
    val kind: ResidentKind,
    val name: String,
    /** The "만나는 환경" sentence shown in the dex. SPEC 4.3 */
    val environment: String,
    /** Short environment name (e.g. "비 오는 날"). Used in the arrival summary title. */
    val envShort: String,
    val probabilityPct: Int?,
    val ceiling: Int?,
    /** Number of misses so far, m */
    val missed: Int,
    val status: ResidentStatus,
    val metDate: String? = null,
    /** Story of the day it was met (village selected-resident card). May contain line breaks. */
    val metStory: String? = null,
) {
    /** Judgings left until the ceiling. 1 if guaranteed on the next judging. */
    val remainingToCeiling: Int? get() = ceiling?.let { it - missed }

    /** Dex number (No.01). */
    val no: String get() = "No.%02d".format(kind.ordinal + 1)
}

@Immutable
data class Deduction(val label: String, val count: Int, val points: Int)

/** Arrival summary state. UI_SPEC 4.1 table. */
enum class ArrivalState { Results, BelowPass, ShortTrip, DailyLimit, NoEnvironment, LaterStop }

@Immutable
data class ArrivalUi(
    val id: String,
    val state: ArrivalState,
    val score: Int,
    val deductions: List<Deduction>,
    val met: List<ResidentUi>,
    val missed: List<ResidentUi>,
    /** Arrival time (e.g. "11월 14일 토요일 오후 1:58") */
    val arrivedAt: String,
    /** Short date (e.g. "11월 14일"). Used on the met-resident card. */
    val dateShort: String,
    /** Driving duration (e.g. "1시간 50분") */
    val duration: String,
    /** Today's environment names (e.g. ["비", "가족 동승"]) */
    val environments: List<String>,
    /** Number of occupants (including the driver) */
    val passengers: Int,
    val points: Int,
    val chancesTotal: Int,
    val chancesLeft: Int,
    /** One not-yet-met resident to show when no environment was fulfilled */
    val hint: ResidentUi? = null,
    /** Deduction items not checked on this vehicle (e.g. 안전벨트) */
    val unchecked: List<String> = emptyList(),
    /** Driving time (minutes, after time scale). Used for "2시간 넘는 운행" in weekly records. */
    val drivingMinutes: Int = 0,
    /** Number of rests (stops longer than REST_MIN) */
    val rests: Int = 0,
)

@Immutable
data class VillageUi(
    val title: String,
    val subtitle: String,
    val points: Int,
    val chancesTotal: Int,
    val chancesLeft: Int,
    val weeklyAverage: Int,
    val weeklyDelta: Int,
    /** Village drawing for the wide (1152×748) and narrow (571×748) layouts */
    val spec: VillageSpec,
    val specNarrow: VillageSpec,
    /** Resident position (tile) in the village → resident */
    val residentsAt: Map<Pair<Int, Int>, ResidentUi>,
)

@Immutable
data class DexUi(val residents: List<ResidentUi>) {
    val available get() = residents.filter { it.status != ResidentStatus.ComingSoon }
    val metCount get() = residents.count { it.status == ResidentStatus.Met }
}

/* ---------------- Records (UI_SPEC 4.6) ---------------- */

/** One row in the trip list. Tapping it reshows that trip's arrival summary without animation. */
@Immutable
data class TripUi(
    val id: String,
    /** e.g. "11월 14일 (토)" */
    val day: String,
    /** e.g. "오후 1:58" */
    val time: String,
    /** Weekday index (Mon = 0 ... Sun = 6). Used for the per-day bars. */
    val weekday: Int,
    val summary: ArrivalUi,
) {
    val score get() = summary.score
    val duration get() = summary.duration
    val environments get() = summary.environments
    val met get() = summary.met
    /** Trips under 10 minutes are excluded from score stats. */
    val counted get() = summary.state != ArrivalState.ShortTrip
    val harshCount get() = summary.deductions.sumOf { it.count }
}

/** One this-week card. [good] means improved over last week (ok color), otherwise warn color. */
@Immutable
data class WeekStat(val label: String, val value: String, val unit: String, val compare: String, val good: Boolean)

/** Safety score per weekday (null if no trips). */
@Immutable
data class DayScore(val label: String, val score: Int?)

@Immutable
data class RecordsUi(
    /** e.g. "11월 9일 ~ 11월 15일" */
    val weekRange: String,
    val stats: List<WeekStat>,
    val days: List<DayScore>,
    val trips: List<TripUi>,
)

/* ---------------- Settings (UI_SPEC 4.7) ---------------- */

/** One row of "이 차량에서 확인하는 항목". */
@Immutable
data class CheckUi(val name: String, val supported: Boolean, val use: String)

@Immutable
data class SettingsUi(val checks: List<CheckUi>, val version: String)
