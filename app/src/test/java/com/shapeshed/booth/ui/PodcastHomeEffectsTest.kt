package com.shapeshed.booth.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodcastHomeEffectsTest {
    @Test
    fun savedTabToRestore_returnsTheSavedTabWithNoDeepLink() {
        assertEquals(
            PodcastTab.UP_NEXT,
            savedTabToRestore(initialEpisodeId = null, savedPodcastTab = "UP_NEXT"),
        )
    }

    @Test
    fun savedTabToRestore_yieldsToANotificationDeepLink() {
        // Regression: the saved-tab restore and a new-episode notification deep link both ran at
        // startup, and a late settings read cleared the episode and left the tap on a list.
        assertNull(savedTabToRestore(initialEpisodeId = 42L, savedPodcastTab = "UP_NEXT"))
    }

    @Test
    fun savedTabToRestore_isNullWhenNoTabWasSaved() {
        assertNull(savedTabToRestore(initialEpisodeId = null, savedPodcastTab = null))
    }

    @Test
    fun savedTabToRestore_ignoresAnUnknownSavedValue() {
        assertNull(savedTabToRestore(initialEpisodeId = null, savedPodcastTab = "NOT_A_TAB"))
    }
}
