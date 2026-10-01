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

    @Test
    fun undoRestoresTheIndexSoTheDatabaseCanBePutBackInTheSameOrder() {
        val removal = requireNotNull(removeQueueItem(listOf("one", "two", "three"), "two"))

        // The index is what the undo hands to the database, so it has to be the one the episode
        // occupied rather than the position it would land in by being appended.
        assertEquals(1, removal.second.originalIndex)
    }

    @Test
    fun undoRestoresAnItemAtTheHeadOfTheQueue() {
        val removal = requireNotNull(removeQueueItem(listOf("one", "two", "three"), "one"))

        assertEquals(0, removal.second.originalIndex)
        assertEquals(listOf("one", "two", "three"), restoreQueueItem(removal.first, removal.second))
    }

    @Test
    fun undoOfAnItemAlreadyRestoredDoesNotDuplicateIt() {
        val removal = requireNotNull(removeQueueItem(listOf("one", "two", "three"), "two"))
        val restored = restoreQueueItem(removal.first, removal.second)

        // Applying the same undo twice must not put two copies of the episode in the queue. Only
        // reachable if a stale undo arrives, which is what a second swipe or a rotation used to
        // make likely.
        assertEquals(listOf("one", "two", "three"), restored)
        assertEquals(3, restored.distinct().size)
    }
}
