package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sleep timer measures playback time, so paused time must not count against the budget.
 *
 * The arithmetic lives in [SleepTimerBudget] with no player and no coroutines, which is what makes
 * this testable at all. Previously it was inline in the media service, where the only way to
 * exercise it was a device.
 */
class SleepTimerBudgetTest {
    @Test
    fun startsWithTheWholeDurationRemaining() {
        val budget = SleepTimerBudget(totalMs = 30 * 60_000L)

        assertEquals(30 * 60_000L, budget.remainingMs)
        assertFalse(budget.isExhausted)
    }

    @Test
    fun consumingSubtractsElapsedPlaybackTime() {
        val budget = SleepTimerBudget(totalMs = 10_000L)

        assertFalse(budget.consume(4_000L))

        assertEquals(6_000L, budget.remainingMs)
    }

    @Test
    fun reportsExhaustedOnceTheBudgetIsSpent() {
        val budget = SleepTimerBudget(totalMs = 10_000L)

        assertFalse(budget.consume(6_000L))
        assertTrue(budget.consume(4_000L))

        assertTrue(budget.isExhausted)
    }

    @Test
    fun staysExhaustedOnceSpent() {
        val budget = SleepTimerBudget(totalMs = 1_000L)
        budget.consume(1_000L)

        assertTrue(budget.consume(1_000L))
        assertEquals(0L, budget.remainingMs)
    }

    /**
     * The old loop subtracted a whole tick whenever the player had been playing at the *start* of
     * it, so playback that stopped mid-tick still charged the full tick. A negative or zero elapsed
     * value is treated as no time passing at all.
     */
    @Test
    fun ignoresNonPositiveElapsedTime() {
        val budget = SleepTimerBudget(totalMs = 5_000L)

        budget.consume(0L)
        budget.consume(-250L)

        assertEquals(5_000L, budget.remainingMs)
    }

    /** The service clamps on the way out so the sheet never renders a negative countdown. */
    @Test
    fun stateNeverReportsNegativeRemaining() {
        val budget = SleepTimerBudget(totalMs = 500L)
        budget.consume(900L)

        assertEquals(0L, budget.state().remainingMs)
        assertEquals(500L, budget.state().totalMs)
    }

    /**
     * A 30 minute timer with the user paused for an hour should still have its full budget: this
     * is the regression that motivated extracting the class, since the old loop simply never
     * advanced while paused.
     */
    @Test
    fun pausedTimeIsNotConsumed() {
        val budget = SleepTimerBudget(totalMs = 30 * 60_000L)

        repeat(1_440) { budget.consume(0L) }

        assertEquals(30 * 60_000L, budget.remainingMs)
        assertFalse(budget.isExhausted)
    }
}
