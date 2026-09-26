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

    @Test
    fun reorderMovesItemDownToTheDraggedTarget() {
        assertEquals(
            listOf("one", "three", "two"),
            reorderQueueItem(listOf("one", "two", "three"), "two", "three"),
        )
    }

    @Test
    fun reorderMovesItemUpToTheDraggedTarget() {
        assertEquals(
            listOf("three", "one", "two"),
            reorderQueueItem(listOf("one", "two", "three"), "three", "one"),
        )
    }

    @Test
    fun reorderLeavesQueueUnchangedWhenTargetIsMissing() {
        val queue = listOf("one", "two", "three")

        assertEquals(queue, reorderQueueItem(queue, "two", "missing"))
    }
}
