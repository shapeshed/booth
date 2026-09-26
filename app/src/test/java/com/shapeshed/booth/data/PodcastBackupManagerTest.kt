package com.shapeshed.booth.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastBackupManagerTest {
    @Test
    fun acceptsCurrentAndOlderFormatsButNotOtherOrFutureFormats() {
        assertTrue(
            PodcastBackupManager.isSupported("booth-backup", 1),
        )
        assertFalse(
            PodcastBackupManager.isSupported("booth-backup", 2),
        )
        assertFalse(
            PodcastBackupManager.isSupported("other-app", 1),
        )
    }
}
