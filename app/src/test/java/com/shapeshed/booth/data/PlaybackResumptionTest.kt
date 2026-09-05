package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackResumptionTest {
    @Test
    fun activeEpisodeIsRestoredBeforeRemainingQueue() {
        assertEquals(
            listOf(20L, 30L, 40L),
            orderedResumptionIds(activeEpisodeId = 20L, queuedEpisodeIds = listOf(30L, 40L)),
        )
    }

    @Test
    fun activeEpisodeIsNotDuplicatedWhenAlreadyInQueue() {
        assertEquals(
            listOf(20L, 30L, 40L),
            orderedResumptionIds(activeEpisodeId = 20L, queuedEpisodeIds = listOf(20L, 30L, 20L, 40L)),
        )
    }
}
