package com.shapeshed.booth.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastQueueStateTest {
    @Test
    fun successfulRemovalLeavesItemOutOfQueue() {
        val removal = requireNotNull(removeQueueItem(listOf("one", "two", "three"), "three"))

        assertEquals(listOf("one", "two"), removal.first)
    }

    @Test
    fun failedRemovalRestoresItemAtOriginalPosition() {
        val removal = requireNotNull(removeQueueItem(listOf("one", "two", "three"), "two"))

        assertEquals(
            listOf("one", "two", "three"),
            restoreQueueItem(removal.first, removal.second),
        )
    }
}
