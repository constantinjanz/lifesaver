package com.lifesaver.domain

import com.lifesaver.data.Baseline
import com.lifesaver.data.DailyStatus
import com.lifesaver.data.Strictness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetEngineTest {
    private val min = 60_000L

    @Test fun normalTimeBurns1x() {
        // 20 min foreground, none on Reels, 30 min budget -> 10 min left.
        assertEquals(10 * min, BudgetEngine.remainingMs(30 * min, 20 * min, 0))
        assertFalse(BudgetEngine.isExhausted(30 * min, 20 * min, 0))
    }

    @Test fun reelsBurns2x() {
        // 10 min total, all on Reels -> effective 20 min. Budget 30 -> 10 left.
        assertEquals(20 * min, BudgetEngine.effectiveBurnMs(10 * min, 10 * min))
        assertEquals(10 * min, BudgetEngine.remainingMs(30 * min, 10 * min, 10 * min))
    }

    @Test fun mixedSurfaces() {
        // 15 min foreground, 5 of them Reels -> 10*1 + 5*2 = 20 effective.
        assertEquals(20 * min, BudgetEngine.effectiveBurnMs(15 * min, 5 * min))
    }

    @Test fun exhaustionAndClamp() {
        assertTrue(BudgetEngine.isExhausted(30 * min, 20 * min, 10 * min)) // 40 effective >= 30
        assertEquals(0, BudgetEngine.remainingMs(30 * min, 20 * min, 10 * min))
    }

    @Test fun remainingFraction() {
        assertEquals(0.5f, BudgetEngine.remainingFraction(30 * min, 15 * min, 0), 0.001f)
    }
}

class FrictionLadderTest {
    @Test fun standardLadderMatchesPrd() {
        assertEquals(5, FrictionLadder.pauseSeconds(1))
        assertEquals(15, FrictionLadder.pauseSeconds(2))
        assertEquals(30, FrictionLadder.pauseSeconds(3))
        assertEquals(30, FrictionLadder.pauseSeconds(9))
    }

    @Test fun strictnessScales() {
        assertTrue(FrictionLadder.pauseSeconds(3, Strictness.GENTLE) < 30)
        assertTrue(FrictionLadder.pauseSeconds(3, Strictness.STRICT) > 30)
    }
}

class StreakCalculatorTest {
    private fun day(k: String, ok: Boolean) = DailyStatus(k, ok, if (ok) "ok" else "over_budget")

    @Test fun consecutiveSuccessesCount() {
        val s = StreakCalculator.compute(
            listOf(
                day("2026-07-18", true),
                day("2026-07-19", true),
                day("2026-07-20", true),
            ),
        )
        assertEquals(3, s.current)
        assertEquals(3, s.longest)
    }

    @Test fun failureBreaksCurrentButLongestRemembered() {
        val s = StreakCalculator.compute(
            listOf(
                day("2026-07-15", true),
                day("2026-07-16", true),
                day("2026-07-17", false),
                day("2026-07-18", true),
            ),
        )
        assertEquals(1, s.current)
        assertEquals(2, s.longest)
    }

    @Test fun calendarGapBreaksCurrent() {
        val s = StreakCalculator.compute(
            listOf(
                day("2026-07-18", true),
                day("2026-07-20", true), // missing the 19th
            ),
        )
        assertEquals(1, s.current)
    }
}

class TimeSavedTest {
    private val min = 60_000L

    @Test fun neverNegative() {
        assertEquals(0, TimeSaved.savedMs(10 * min, 40 * min))
    }

    @Test fun formatting() {
        assertEquals("6H 20M", TimeSaved.formatHm(380 * min))
        assertEquals("45M", TimeSaved.formatHm(45 * min))
    }

    @Test fun workoutsTangible() {
        assertEquals("≈ 4 WORKOUTS", TimeSaved.tangible(180 * min))
    }
}

class SelfBindingTest {
    private val now = 1_000_000L

    @Test fun budgetIncreaseIsDelayed() {
        val d = SelfBinding.budgetChange(30, 45, now)
        assertEquals(SelfBinding.Direction.LOOSEN, d.direction)
        assertEquals(now + SelfBinding.DELAY_MS, d.effectiveTs)
        assertFalse(d.isImmediate)
    }

    @Test fun budgetDecreaseIsImmediate() {
        val d = SelfBinding.budgetChange(30, 20, now)
        assertEquals(SelfBinding.Direction.TIGHTEN, d.direction)
        assertEquals(now, d.effectiveTs)
        assertTrue(d.isImmediate)
    }

    @Test fun removingTargetAppIsDelayed() {
        val d = SelfBinding.targetAppsChange(setOf("a", "b"), setOf("a"), now)
        assertEquals(SelfBinding.Direction.LOOSEN, d.direction)
    }
}

class BaselineModelTest {
    @Test fun seedsThenRefinesThenFixes() {
        var b = BaselineModel.refine(null, "ig", "weekday", 100)
        assertEquals(100, b.avgMs)
        assertEquals(1, b.sampleDays)
        b = BaselineModel.refine(b, "ig", "weekday", 200) // avg 150
        assertEquals(150, b.avgMs)
        // Push to the cap.
        var fixed = Baseline("ig", "weekday", 500, BaselineModel.MAX_SAMPLE_DAYS)
        fixed = BaselineModel.refine(fixed, "ig", "weekday", 9999)
        assertEquals(500, fixed.avgMs) // unchanged once fixed
    }
}

class ScheduleBlockTest {
    @Test fun withinDaytimeWindow() {
        val win = BlockWindow(7 * 60, 10 * 60) // 07:00–10:00
        assertTrue(ScheduleBlock.isBlocked(win, 8 * 60))
        assertFalse(ScheduleBlock.isBlocked(win, 10 * 60)) // end exclusive
        assertFalse(ScheduleBlock.isBlocked(win, 6 * 60 + 59))
    }

    @Test fun wrapsPastMidnight() {
        val win = BlockWindow(22 * 60, 6 * 60) // 22:00–06:00
        assertTrue(ScheduleBlock.isBlocked(win, 23 * 60))
        assertTrue(ScheduleBlock.isBlocked(win, 2 * 60))
        assertFalse(ScheduleBlock.isBlocked(win, 12 * 60))
    }

    @Test fun disabledWhenEqual() {
        assertFalse(ScheduleBlock.isBlocked(BlockWindow(60, 60), 60))
        assertFalse(ScheduleBlock.isBlocked(null, 60))
    }
}

class DayKeysTest {
    @Test fun weekdayGroup() {
        assertEquals("weekday", DayKeys.weekdayGroup("2026-07-20")) // Monday
        assertEquals("weekend", DayKeys.weekdayGroup("2026-07-19")) // Sunday
    }

    @Test fun weekKeyIsMonday() {
        assertEquals("2026-07-20", DayKeys.weekKeyOf("2026-07-22")) // Wed -> Mon of that week
        assertEquals("2026-07-20", DayKeys.weekKeyOf("2026-07-20"))
    }
}
