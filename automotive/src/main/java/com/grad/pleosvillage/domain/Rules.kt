package com.grad.pleosvillage.domain

/*
 * Judging rule constants and the resident table. docs/SPEC.md §3, §4, §5, §9. No Android imports (verified by JVM unit tests).
 * All times are "vehicle time" milliseconds: real time multiplied by the time scale (DEMO_TIME_SCALE).
 * The 1 s / 3 s of harsh steering and the 10 s seatbelt grace period are real time (SPEC §3).
 */
object Rules {
    const val MIN = 60_000L

    const val TRIP_END_GAP = 30 * MIN
    const val REST_MIN = 10 * MIN
    const val MIN_TRIP_DRIVING = 10 * MIN
    const val SCORE_PASS = 80
    const val DAILY_JUDGE_LIMIT = 2
    const val LONG_TRIP_MIN = 120 * MIN

    const val HARSH_ANGLE = 90f
    const val HARSH_WINDOW_MS = 1_000L
    const val HARSH_COOLDOWN_MS = 3_000L
    const val SPEED_FILTER_KMH = 20f

    const val PENALTY_HARSH = 10

    /** Points: score 90–100 +20, 80–89 +10 (SPEC §5). */
    fun pointsFor(score: Int) = when {
        score >= 90 -> 20
        score >= SCORE_PASS -> 10
        else -> 0
    }

    fun score(harshSteer: Int, harshBrake: Int) = (100 - PENALTY_HARSH * (harshSteer + harshBrake)).coerceAtLeast(0)
}

/** Environment a resident needs (judged from trip accumulations). */
enum class Env { RAIN, FAMILY, NIGHT, WINTER, LONG_TRIP, CHARGE, LOCATION }

/**
 * Resident definition. SPEC 4.3 table.
 * @param id storage key (rain, family, ...). Uses the same order and names as the screen's ResidentKind.
 * @param envMinMs environment threshold (total driving time in that condition)
 */
data class ResidentRule(
    val id: String,
    val env: Env,
    val probabilityPct: Int,
    val ceiling: Int,
    val envMinMs: Long,
    /** false if M2 or later, or out of scope. Not judged. */
    val judged: Boolean,
)

object ResidentTable {
    val all = listOf(
        ResidentRule("rain", Env.RAIN, 40, 3, 5 * Rules.MIN, judged = true),
        ResidentRule("night", Env.NIGHT, 30, 4, 10 * Rules.MIN, judged = true),
        ResidentRule("family", Env.FAMILY, 50, 2, 10 * Rules.MIN, judged = true),
        ResidentRule("winter", Env.WINTER, 30, 4, 1 * Rules.MIN, judged = true),
        // Long trip (M2): 2+ hours of driving in the trip + at least 1 rest. Checked in judging below instead of via envMinMs.
        ResidentRule("longtrip", Env.LONG_TRIP, 50, 2, Rules.LONG_TRIP_MIN, judged = true),
        // Charge (M2): judged when charging starts. Charging values aren't read yet, so it isn't judged.
        ResidentRule("charge", Env.CHARGE, 30, 4, 0, judged = false),
        ResidentRule("sea", Env.LOCATION, 0, 0, 0, judged = false),
        ResidentRule("mountain", Env.LOCATION, 0, 0, 0, judged = false),
    )

    fun byId(id: String) = all.first { it.id == id }
}

/** Time source. Replaced in tests. */
interface TimeSource {
    /** Monotonic time (SystemClock.elapsedRealtime). */
    fun nowMs(): Long

    /** Today's date key (yyyy-MM-dd). Used for the daily judging limit. */
    fun today(): String

    /** Wall clock (System.currentTimeMillis). Used for displayed dates/times. */
    fun wallMs(): Long
}

/** Random source. Integer in [0, 100). Fixed in tests. */
fun interface RandomSource {
    fun nextPercent(): Int
}
