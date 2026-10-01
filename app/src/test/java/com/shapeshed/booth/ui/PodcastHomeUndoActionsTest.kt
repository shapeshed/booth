package com.shapeshed.booth.ui

import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shape every destructive swipe depends on: act now, offer undo, and never offer an undo for
 * something that did not happen.
 *
 * Framework-free by construction, which is what makes this a JVM test rather than an instrumented
 * one. `UndoSnackbarsTest` covers how offers replace each other; this covers whether one is offered
 * at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PodcastHomeUndoActionsTest {
    @Test
    fun removalIsAppliedAndThenOfferedForUndo() = withActions { actions, presenter, _ ->
        var applied = false

        actions.requestRemoval(
            message = "removed",
            apply = {
                applied = true
                true
            },
            undo = {},
        )

        assertTrue(applied)
        assertEquals(listOf("removed"), presenter.messages)
    }

    @Test
    fun takingTheUndoRunsOnlyTheUndo() = withActions { actions, presenter, _ ->
        var undone = false

        actions.requestRemoval("removed", apply = { true }, undo = { undone = true })
        presenter.take(0)

        assertTrue(undone)
    }

    @Test
    fun aRemovalThatDidNotHappenIsNotOfferedForUndo() = withActions { actions, presenter, _ ->
        var applied = false

        // apply reporting false is how the queue screen says it rolled itself back after a failed
        // write. Offering an undo here would let the listener "undo" a removal that never occurred.
        actions.requestRemoval(
            message = "removed",
            apply = {
                applied = true
                false
            },
            undo = {},
        )

        assertTrue(applied)
        assertEquals(emptyList<String>(), presenter.messages)
    }

    @Test
    fun dismissingAnInboxEpisodeRestoresItOnUndo() = withActions { actions, presenter, calls ->
        actions.dismissInboxEpisode(episode(7L))

        assertEquals(listOf("dismiss:7"), calls.dismissed)
        assertEquals(listOf("Removed from Inbox"), presenter.messages)

        presenter.take(0)

        assertEquals(listOf("restore:7"), calls.restored)
    }

    @Test
    fun unfollowingFromTheSwipeResubscribesOnUndo() = withActions { actions, presenter, calls ->
        actions.unfollowPodcastWithUndo(podcast(), remove = {
            calls.removed += "1"
        }, restore = { calls.restored += "1" })

        assertEquals(listOf("1"), calls.removed)
        assertEquals(listOf("Unfollowed"), presenter.messages)
        assertEquals(emptyList<String>(), calls.restored)

        presenter.take(0)

        assertEquals(listOf("1"), calls.restored)
    }

    @Test
    fun anUnfollowThatTimedOutIsNotUndone() = withActions { actions, presenter, calls ->
        actions.unfollowPodcastWithUndo(podcast(), remove = {
            calls.removed += "1"
        }, restore = { calls.restored += "1" })

        presenter.expire(0)

        assertEquals(listOf("1"), calls.removed)
        assertEquals(emptyList<String>(), calls.restored)
    }

    @Test
    fun removingASelectionIsOneActionThatOneUndoTakesBack() = withActions { actions, presenter, calls ->
        var selectionCleared = false

        actions.removeSelectedInboxEpisodes(
            inbox = listOf(episode(1L), episode(2L), episode(3L)),
            selectedIds = setOf(1L, 3L),
            clearSelection = { selectionCleared = true },
        )

        assertEquals(listOf("dismiss:1", "dismiss:3"), calls.dismissed)
        assertTrue(selectionCleared)
        assertEquals(listOf("2 episodes removed from Inbox"), presenter.messages)

        presenter.take(0)

        // Both come back together: undoing one of five swipes out of a selection would leave the
        // listener no way to tell which four they meant.
        assertEquals(listOf("restore:1", "restore:3"), calls.restored)
    }

    @Test
    fun removingAnEmptySelectionOnlyClearsIt() = withActions { actions, presenter, calls ->
        var selectionCleared = false

        actions.removeSelectedInboxEpisodes(
            inbox = listOf(episode(1L)),
            selectedIds = emptySet(),
            clearSelection = { selectionCleared = true },
        )

        assertTrue(selectionCleared)
        assertEquals(emptyList<String>(), calls.dismissed)
        assertEquals(emptyList<String>(), presenter.messages)
    }

    @Test
    fun aRemovalOfASingleSelectedEpisodeReadsAsOne() = withActions { actions, presenter, _ ->
        actions.removeSelectedInboxEpisodes(
            inbox = listOf(episode(1L)),
            selectedIds = setOf(1L),
            clearSelection = {},
        )

        assertEquals(listOf("1 episode removed from Inbox"), presenter.messages)
    }

    @Test
    fun addingToUpNextOffersUndoOnlyAfterSuccess() = withActions { actions, presenter, calls ->
        actions.requestAddToQueue(7L)

        assertEquals(listOf("7"), calls.added)
        assertEquals(emptyList<String>(), presenter.messages)

        calls.addSuccess?.invoke()

        assertEquals(listOf("Added to Up next"), presenter.messages)
        assertEquals(emptyList<String>(), calls.removedFromQueue)

        presenter.take(0)

        assertEquals(listOf("7"), calls.removedFromQueue)
    }

    @Test
    fun failingToAddToUpNextReportsItWithNoUndo() = withActions { actions, presenter, calls ->
        actions.requestAddToQueue(7L)

        calls.addError?.invoke()

        assertEquals(listOf("Could not add to Up next"), presenter.messages)
    }

    @Test
    fun failingToUndoAnAddReportsTheRemovalError() = withActions { actions, presenter, calls ->
        calls.undoAddResult = Result.failure(IllegalStateException("write failed"))
        actions.requestAddToQueue(7L)
        calls.addSuccess?.invoke()

        presenter.take(0)

        assertEquals(
            listOf("Added to Up next", "Could not remove from Up next"),
            presenter.messages,
        )
    }

    private fun withActions(
        block: (
            actions: PodcastHomeUndoActions,
            presenter: RecordingPresenter,
            calls: RecordedCalls,
        ) -> Unit,
    ) = runTest {
        val presenter = RecordingPresenter()
        val calls = RecordedCalls()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        val strings = PodcastHomeUndoStrings(
            undoLabel = "Undo",
            removedFromInbox = "Removed from Inbox",
            episodesRemovedFromInbox = { count ->
                if (count == 1) "$count episode removed from Inbox" else "$count episodes removed from Inbox"
            },
            addToNextFailed = "Could not add to Up next",
            addedToNext = "Added to Up next",
            removeFromNextFailed = "Could not remove from Up next",
            unfollowed = "Unfollowed",
            downloadRemoved = "Download removed",
            removeDownloadFailed = "Could not remove download",
        )
        try {
            block(
                PodcastHomeUndoActions(
                    scope = scope,
                    snackbars = UndoSnackbars(scope, presenter),
                    strings = strings,
                    addToQueueFromInbox = { episodeId, onAdded, onError ->
                        calls.added += episodeId.toString()
                        calls.addSuccess = onAdded
                        calls.addError = onError
                    },
                    undoAddToQueueFromInbox = { episodeId ->
                        calls.removedFromQueue += episodeId.toString()
                        calls.undoAddResult
                    },
                    dismissFromInbox = { calls.dismissed += "dismiss:$it" },
                    restoreToInbox = { calls.restored += "restore:$it" },
                ),
                presenter,
                calls,
            )
        } finally {
            scope.cancel()
        }
    }

    private class RecordedCalls {
        val added = mutableListOf<String>()
        val dismissed = mutableListOf<String>()
        val restored = mutableListOf<String>()
        val removed = mutableListOf<String>()
        val removedFromQueue = mutableListOf<String>()
        var addSuccess: (() -> Unit)? = null
        var addError: (() -> Unit)? = null
        var undoAddResult: Result<Unit> = Result.success(Unit)
    }

    private class RecordingPresenter : UndoSnackbarPresenter {
        val messages = mutableListOf<String>()
        private val held = mutableListOf<CompletableDeferred<Boolean>>()

        override suspend fun present(offer: UndoOffer): Boolean {
            val result = CompletableDeferred<Boolean>()
            held += result
            messages += offer.message
            return result.await()
        }

        fun take(index: Int) = held[index].complete(true).let { }

        fun expire(index: Int) = held[index].complete(false).let { }
    }

    private fun episode(id: Long) = EpisodeEntity(
        id = id,
        podcastId = 1L,
        guid = "episode-$id",
        title = "Episode $id",
        descriptionHtml = null,
        audioUrl = "https://example.com/$id.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = null,
        positionMs = 0L,
        completed = false,
        localUri = null,
        inInbox = true,
        firstSeenAtMillis = null,
    )

    private fun podcast() = PodcastEntity(
        id = 1L,
        title = "Test podcast",
        author = "Test author",
        feedUrl = "https://example.com/feed.xml",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
        subscribedAtMillis = 1L,
        lastRefreshMillis = null,
    )
}
