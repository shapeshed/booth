package com.shapeshed.booth.ui

import com.shapeshed.booth.data.SleepTimerState
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A guard on where high-frequency values are collected.
 *
 * Booth has regressed this twice, through two different doors, and both times the whole home screen
 * recomposed several times a second:
 *
 * - The 2 Hz playback position was collected in the home screen body. `PlaybackProgress` was then
 *   split out of `PlaybackUiState` so it could be collected in the subtree that draws the scrubber.
 * - The 4 Hz sleep timer was then collected in the home screen body, on its way to the overlay,
 *   while every other value around it was fine.
 *
 * Neither is visible in a screenshot or a functional test, and the cost is battery rather than
 * correctness, so the practical guard is to pin the shape of the type the home screen builds in one
 * pass. A field here is collected once for the whole screen; the fix for either bug was to stop
 * doing that and collect the flow in the subtree that needs it.
 *
 * Uses plain reflection rather than kotlin-reflect, which this project does not depend on.
 */
class PodcastHomeUiStateFrequencyTest {
    /** Types that change faster than a person could notice, so cannot live in a whole-screen state. */
    private val fastMovingTypes: List<Class<*>> = listOf(
        SleepTimerState::class.java,
        PlaybackProgress::class.java,
    )

    @Test
    fun theHomeScreenStateCarriesNothingThatTicksFasterThanASecond() {
        val offenders = PodcastHomeUiState::class.java.declaredFields
            .filter { field -> fastMovingTypes.any { it == field.type } }
            .map { it.name }

        assertTrue(
            "PodcastHomeUiState is collected once for the whole home screen, so a value ticking " +
                "several times a second belongs in the subtree that draws it, not here. Found: " +
                offenders.joinToString(),
            offenders.isEmpty(),
        )
    }

    @Test
    fun theSleepTimerIsNotTheSameTypeAsAnythingItIsGuardedAgainst() {
        // If SleepTimerState were ever folded into a type this guard already handles, the check
        // above would pass for the wrong reason. Cheap, and it keeps the guard honest.
        assertTrue(
            "the two fast-moving types must be distinguishable for this guard to mean anything",
            SleepTimerState::class.java != PlaybackProgress::class.java,
        )
    }
}
