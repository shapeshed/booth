package com.shapeshed.booth.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastRefreshCoordinatorTest {
    @Test
    fun refreshBlocksDoNotOverlap() = runTest {
        val coordinator = PodcastRefreshCoordinator()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        var activeBlocks = 0
        var overlapped = false

        val first = async {
            coordinator.withExclusiveRefresh {
                activeBlocks++
                firstStarted.complete(Unit)
                releaseFirst.await()
                activeBlocks--
            }
        }
        firstStarted.await()
        val second = async {
            coordinator.withExclusiveRefresh {
                activeBlocks++
                if (activeBlocks > 1) overlapped = true
                secondStarted.complete(Unit)
                activeBlocks--
            }
        }

        runCurrent()
        assertFalse(secondStarted.isCompleted)

        releaseFirst.complete(Unit)
        first.await()
        second.await()

        assertTrue(secondStarted.isCompleted)
        assertFalse(overlapped)
    }

    @Test
    fun cancellationReleasesRefreshLock() = runTest {
        val coordinator = PodcastRefreshCoordinator()
        val entered = CompletableDeferred<Unit>()
        val first = launch {
            coordinator.withExclusiveRefresh {
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        entered.await()

        first.cancelAndJoin()
        var secondEntered = false
        coordinator.withExclusiveRefresh { secondEntered = true }

        assertTrue(secondEntered)
    }
}
