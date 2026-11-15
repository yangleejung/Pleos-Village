package com.grad.pleosvillage.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests from SPEC §11. Time and randomness are fixed. */
class EngineTest {

    private class FakeTime(var now: Long = 0, var day: String = "2026-11-14") : TimeSource {
        override fun nowMs() = now
        override fun today() = day
        override fun wallMs() = now
        fun advanceMin(min: Long, scale: Int = 60) { now += min * 60_000L / scale }
        fun advanceMs(ms: Long) { now += ms }
    }

    /** Demo settings: scale 60, speed filter off. Random defaults to 99 (= never met by probability). */
    private fun engine(rand: Int = 99, time: FakeTime = FakeTime()) =
        Engine(EngineState(settings = Settings(timeScale = 60, speedFilter = false)), time, { rand }) to time

    private fun Engine.drive() = onGear(driving = true, parked = false)
    private fun Engine.park() = onGear(driving = false, parked = true)!!

    @Test fun score_boundary() {
        assertEquals(100, Rules.score(0, 0))
        assertEquals(80, Rules.score(1, 1))
        assertEquals(70, Rules.score(2, 1))
        assertEquals(0, Rules.score(20, 0))
        assertEquals(20, Rules.pointsFor(90))
        assertEquals(10, Rules.pointsFor(80))
        assertEquals(0, Rules.pointsFor(79))
    }

    @Test fun short_trip_is_not_judged() {
        val (e, t) = engine()
        e.drive(); t.advanceMin(9); val s = e.park()
        assertEquals(StopState.SHORT_TRIP, s.state)
        assertEquals(0, e.state.judgeCount["2026-11-14"] ?: 0)
    }

    /** Scenario A: demo data (just before ceiling) + rear seat occupied + wiper 15 min → meet 레이니 and 해삐, score 100, +20. */
    @Test fun scenario_A_rain_and_family() {
        val (e, t) = engine()
        e.loadDemo()
        e.onSeats(rear = true, passengers = 2)
        e.drive()
        e.onWiper(true)
        t.advanceMin(15)
        val s = e.park()
        assertEquals(StopState.RESULTS, s.state)
        assertEquals(100, s.score)
        assertEquals(20, s.points)
        assertEquals(setOf("rain", "family"), s.met.toSet())
        assertTrue(e.state.residents.getValue("rain").met)
    }

    /** Scenario B: harsh steering x2 + harsh braking x1 → score 70, no residents, miss counts unchanged. */
    @Test fun scenario_B_below_pass() {
        val (e, t) = engine()
        e.loadDemo()
        e.onSeats(rear = true, passengers = 2)
        e.drive(); e.onWiper(true)
        t.advanceMin(5)
        e.onSteering(0f); t.advanceMs(200); e.onSteering(120f)       // #1
        t.advanceMs(3_500); e.onSteering(-10f)                       // #2 (after 3 s cooldown)
        e.onBrake(true); e.onBrake(false)                            // harsh braking #1
        t.advanceMin(10)
        val s = e.park()
        assertEquals(70, s.score)
        assertEquals(StopState.BELOW_PASS, s.state)
        assertTrue(s.met.isEmpty())
        assertEquals(2, e.state.residents.getValue("rain").missed)
    }

    @Test fun harsh_steering_needs_90_degrees_within_1s_and_cooldown() {
        val (e, t) = engine()
        e.drive()
        e.onSteering(0f); t.advanceMs(1_500); e.onSteering(80f)   // 80°: not harsh
        t.advanceMs(1_500); e.onSteering(-20f)                    // 80 one second ago → -20 = 100°: #1
        t.advanceMs(500); e.onSteering(90f)                       // within cooldown: not counted
        assertEquals(1, e.state.trip!!.harshSteer)
    }

    /** The first maneuver right after a data reset still counts (the angle received before reset is the reference). Scenario B regression. */
    @Test fun harsh_steering_after_reset_uses_previous_angle() {
        val (e, t) = engine()
        e.onSteering(0f)                                          // current angle received at app start
        t.advanceMs(5_000)
        e.resetAll()
        e.drive(); t.advanceMs(3_000)
        e.onSteering(-200f)                                       // values arrive only on change, so 0 → -200 in one step
        assertEquals(1, e.state.trip!!.harshSteer)
    }

    @Test fun ceiling_forces_meet_and_probability_otherwise() {
        val (e, t) = engine(rand = 99)
        e.drive(); e.onWiper(true); t.advanceMin(15)
        val s1 = e.park()
        assertEquals(listOf("rain" to 2), s1.missed)                 // 40% failed, m=1, 2 left until ceiling
        val (e2, t2) = engine(rand = 10)
        e2.drive(); e2.onWiper(true); t2.advanceMin(15)
        assertEquals(listOf("rain"), e2.park().met)                  // 10 < 40 → met
    }

    @Test fun daily_limit_two_judging_trips() {
        val (e, t) = engine()
        repeat(2) {
            e.drive(); t.advanceMin(15); e.park(); e.endTrip()
        }
        e.drive(); e.onWiper(true); t.advanceMin(15)
        val s = e.park()
        assertEquals(StopState.DAILY_LIMIT, s.state)
        assertEquals(0, e.state.residents.getValue("rain").missed)  // over the limit, the ceiling count isn't raised either
    }

    @Test fun resident_judged_once_per_trip_and_rest_counts() {
        val (e, t) = engine(rand = 99)
        e.drive(); e.onWiper(true); t.advanceMin(15)
        e.park()
        t.advanceMin(15)                                             // 15 min parked = rest; under 30 min, so same trip
        e.drive(); t.advanceMin(5)
        val s2 = e.park()
        assertTrue(s2.missed.none { it.first == "rain" })            // not judged again in the same trip
        assertEquals(1, e.state.trip!!.rests)
    }

    @Test fun trip_ends_after_30_minutes_parked() {
        val (e, t) = engine()
        e.drive(); t.advanceMin(15); e.park()
        val first = e.state.trip!!.id
        t.advanceMin(31)
        e.drive()
        assertTrue(e.state.trip!!.id != first)
    }

    @Test fun wiper_threshold_boundary_4_vs_5_minutes() {
        val (e, t) = engine()
        e.drive(); t.advanceMin(10); e.onWiper(true); t.advanceMin(4); e.onWiper(false); t.advanceMin(1)
        assertTrue(Env.RAIN !in e.park().envs)
        val (e2, t2) = engine()
        e2.drive(); t2.advanceMin(10); e2.onWiper(true); t2.advanceMin(5)
        assertTrue(Env.RAIN in e2.park().envs)
    }

    @Test fun unsupported_resident_not_judged() {
        val t = FakeTime()
        val e = Engine(EngineState(settings = Settings(60, false)), t, { 0 }, unsupported = { setOf(Env.RAIN) })
        e.onGear(true, false); e.onWiper(true); t.advanceMin(15)
        assertNull(e.onGear(false, true)!!.met.firstOrNull { it == "rain" })
    }
}
