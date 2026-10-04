package com.shapeshed.booth.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastNotificationActionsTest {
    @Test
    fun addToQueueActionIsHiddenWhenEpisodeIsAlreadyQueued() {
        assertFalse(shouldShowAddToQueueNotificationAction(episodeId = 42L, queuedEpisodeIds = setOf(42L)))
    }

    @Test
    fun addToQueueActionIsShownWhenEpisodeIsNotQueued() {
        assertTrue(shouldShowAddToQueueNotificationAction(episodeId = 42L, queuedEpisodeIds = setOf(7L)))
    }
}
