package com.shapeshed.booth.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastRefreshProgressTest {
    @Test
    fun fetchingWithKnownTotalIsDeterminate() {
        val progress = PodcastRefreshProgress(completed = 3, total = 4)

        assertTrue(progress.isDeterminate)
        assertEquals(0.75f, progress.fraction)
    }

    @Test
    fun finalizingIsIndeterminateEvenWhenEveryFeedCompleted() {
        val progress = PodcastRefreshProgress(
            completed = 4,
            total = 4,
            phase = PodcastRefreshPhase.FINALIZING,
        )

        assertFalse(progress.isDeterminate)
        assertEquals(1f, progress.fraction)
    }

    @Test
    fun unknownTotalIsIndeterminate() {
        assertFalse(PodcastRefreshProgress().isDeterminate)
    }
}
