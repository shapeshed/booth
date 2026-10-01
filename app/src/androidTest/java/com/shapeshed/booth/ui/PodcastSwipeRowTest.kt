package com.shapeshed.booth.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.DownloadAssetType
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.ui.theme.BoothAppTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The gesture, driven for real rather than through a seam.
 *
 * The previous test drove `PodcastSwipeRow` through an `initiallyShowRemovalConfirmation` flag, so it
 * never performed a swipe: `onDismiss`, the direction branches, the positional threshold and the
 * `reset()` that makes the row reusable were all unverified. Those are exactly the parts that rot,
 * because a swipe row that stays dismissed looks fine in a screenshot and is dead to the finger.
 *
 * Every test here swipes twice. The second swipe is the point: it fails unless the first one left the
 * row settled, which is what a gesture-driven test is able to say and a flag-driven one could not.
 */
class PodcastSwipeRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun swipingAPodcastOffRemovesItAndTheRowIsSwipeableAgain() {
        val removed = mutableListOf<Long>()

        composeRule.setContent {
            BoothAppTheme {
                PodcastSwipeRow(
                    podcast = podcast(),
                    onClick = {},
                    onRemove = { removed += 1L },
                )
            }
        }

        swipeRowLeft()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(listOf(1L), removed) }

        // A row left dismissed cannot be swiped again, so this only passes because the first swipe
        // reset it back to settled.
        swipeRowLeft()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(listOf(1L, 1L), removed) }
    }

    @Test
    fun swipingAPodcastOffDoesNotAskForConfirmation() {
        val removed = mutableListOf<Long>()

        composeRule.setContent {
            BoothAppTheme {
                PodcastSwipeRow(
                    podcast = podcast(),
                    onClick = {},
                    onRemove = { removed += 1L },
                )
            }
        }

        swipeRowLeft()
        composeRule.waitForIdle()

        // The undo snackbar replaces the dialog. A dialog here means the confirmation is back, and
        // the listener pays two taps for a mistake instead of one.
        composeRule.onNodeWithText("Remove Test podcast").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(listOf(1L), removed) }
    }

    @Test
    fun tappingAPodcastStillOpensIt() {
        var opened = 0
        var removed = 0

        composeRule.setContent {
            BoothAppTheme {
                PodcastSwipeRow(
                    podcast = podcast(),
                    onClick = { opened++ },
                    onRemove = { removed++ },
                )
            }
        }

        composeRule.onNodeWithText("Test podcast").performClick()

        composeRule.runOnIdle {
            assertEquals(1, opened)
            assertEquals(0, removed)
        }
    }

    @Test
    fun swipingAQueueEpisodeOffRemovesItAndTheRowIsSwipeableAgain() {
        val removed = mutableListOf<Long>()

        composeRule.setContent {
            BoothAppTheme {
                QueueEpisodeSwipeRow(
                    episode = episode(1L),
                    podcastTitle = "Test podcast",
                    active = false,
                    dragging = false,
                    progressOverride = null,
                    positionOverride = null,
                    isPlaying = false,
                    isBuffering = false,
                    downloadProgress = null,
                    onOpen = {},
                    onPlay = {},
                    onDownload = {},
                    onLongPress = {},
                    onRemove = { removed += 1L },
                    showDragHandle = false,
                )
            }
        }

        swipeRowLeft()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(listOf(1L), removed) }

        swipeRowLeft()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(listOf(1L, 1L), removed) }
    }

    @Test
    fun swipingAQueueEpisodeOffRemovesItWithoutAskingFirst() {
        val removed = mutableListOf<Long>()

        composeRule.setContent {
            BoothAppTheme {
                QueueEpisodeSwipeRow(
                    episode = episode(1L),
                    podcastTitle = "Test podcast",
                    active = false,
                    dragging = false,
                    progressOverride = null,
                    positionOverride = null,
                    isPlaying = false,
                    isBuffering = false,
                    downloadProgress = null,
                    onOpen = {},
                    onPlay = {},
                    onDownload = {},
                    onLongPress = {},
                    onRemove = { removed += 1L },
                    showDragHandle = false,
                )
            }
        }

        swipeRowLeft()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Remove Episode 1").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(listOf(1L), removed) }
    }

    @Test
    fun swipingAnInboxEpisodeLeftDismissesItAndTheRowIsSwipeableAgain() {
        val dismissed = mutableListOf<Long>()

        composeRule.setContent {
            BoothAppTheme {
                InboxEpisodeSwipeRow(
                    episode = episode(1L),
                    podcastTitle = "Test podcast",
                    active = false,
                    selected = false,
                    selectionMode = false,
                    onOpen = {},
                    onAddToQueue = {},
                    onDismiss = { dismissed += 1L },
                    onLongPress = {},
                    onActions = {},
                    onToggleSelection = {},
                    onPlay = {},
                    onDownload = {},
                    isPlaying = false,
                    isBuffering = false,
                    positionOverride = null,
                    downloadProgress = null,
                )
            }
        }

        swipeRowLeft()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(listOf(1L), dismissed) }

        swipeRowLeft()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(listOf(1L, 1L), dismissed) }
    }

    @Test
    fun swipingAnInboxEpisodeRightAddsItToUpNextRatherThanDismissingIt() {
        val dismissed = mutableListOf<Long>()
        val added = mutableListOf<Long>()

        composeRule.setContent {
            BoothAppTheme {
                InboxEpisodeSwipeRow(
                    episode = episode(1L),
                    podcastTitle = "Test podcast",
                    active = false,
                    selected = false,
                    selectionMode = false,
                    onOpen = {},
                    onAddToQueue = { added += 1L },
                    onDismiss = { dismissed += 1L },
                    onLongPress = {},
                    onActions = {},
                    onToggleSelection = {},
                    onPlay = {},
                    onDownload = {},
                    isPlaying = false,
                    isBuffering = false,
                    positionOverride = null,
                    downloadProgress = null,
                )
            }
        }

        swipeRowRight()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(listOf(1L), added)
            // The two directions must not have crossed over, which is the regression this pins.
            assertEquals(emptyList<Long>(), dismissed)
        }
    }

    @Test
    fun anInboxRowInSelectionModeDoesNotSwipe() {
        val dismissed = mutableListOf<Long>()

        composeRule.setContent {
            BoothAppTheme {
                InboxEpisodeSwipeRow(
                    episode = episode(1L),
                    podcastTitle = "Test podcast",
                    active = false,
                    selected = false,
                    selectionMode = true,
                    onOpen = {},
                    onAddToQueue = {},
                    onDismiss = { dismissed += 1L },
                    onLongPress = {},
                    onActions = {},
                    onToggleSelection = {},
                    onPlay = {},
                    onDownload = {},
                    isPlaying = false,
                    isBuffering = false,
                    positionOverride = null,
                    downloadProgress = null,
                )
            }
        }

        swipeRowLeft()
        swipeRowRight()
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(emptyList<Long>(), dismissed) }
    }

    @Test
    fun swipingADownloadKeepsItUntilSnackbarExpires() {
        var removed = 0
        val presenter = PendingUndoSnackbarPresenter()
        val pendingIds = mutableStateOf(emptySet<Long>())

        composeRule.setContent {
            BoothAppTheme {
                val scope = rememberCoroutineScope()
                PodcastDownloadsScreen(
                    assets = listOf(asset(1L)),
                    episodes = mapOf(1L to episode(1L)),
                    podcastsById = mapOf(1L to podcast()),
                    activeEpisodeId = null,
                    isPlaying = false,
                    isBuffering = false,
                    downloadProgress = emptyMap(),
                    onOpen = {},
                    onPlay = {},
                    onDownload = {},
                    onRemove = {
                        removed++
                        Result.success(Unit)
                    },
                    onLongPress = {},
                    undoActions = rememberDownloadUndoActions(scope, presenter),
                    pendingRemovalEpisodeIds = pendingIds.value,
                    onPendingRemovalEpisodeChange = { id, pending ->
                        pendingIds.value = if (pending) pendingIds.value + id else pendingIds.value - id
                    },
                )
            }
        }

        swipeRowLeft()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Episode 1").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, removed) }
        presenter.resolve(undo = false)
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(1, removed) }
    }

    @Test
    fun undoingADownloadSwipeRestoresTheRowWithoutDeletingIt() {
        var removed = 0
        val presenter = PendingUndoSnackbarPresenter()
        val pendingIds = mutableStateOf(emptySet<Long>())

        composeRule.setContent {
            BoothAppTheme {
                val scope = rememberCoroutineScope()
                PodcastDownloadsScreen(
                    assets = listOf(asset(1L)),
                    episodes = mapOf(1L to episode(1L)),
                    podcastsById = mapOf(1L to podcast()),
                    activeEpisodeId = null,
                    isPlaying = false,
                    isBuffering = false,
                    downloadProgress = emptyMap(),
                    onOpen = {},
                    onPlay = {},
                    onDownload = {},
                    onRemove = {
                        removed++
                        Result.success(Unit)
                    },
                    onLongPress = {},
                    undoActions = rememberDownloadUndoActions(scope, presenter),
                    pendingRemovalEpisodeIds = pendingIds.value,
                    onPendingRemovalEpisodeChange = { id, pending ->
                        pendingIds.value = if (pending) pendingIds.value + id else pendingIds.value - id
                    },
                )
            }
        }

        swipeRowLeft()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Episode 1").assertDoesNotExist()
        presenter.resolve(undo = true)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Episode 1").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, removed) }
    }

    @androidx.compose.runtime.Composable
    private fun rememberDownloadUndoActions(
        scope: CoroutineScope,
        presenter: PendingUndoSnackbarPresenter,
    ): PodcastHomeUndoActions = remember(scope, presenter) {
        PodcastHomeUndoActions(
            scope = scope,
            snackbars = UndoSnackbars(scope, presenter),
            strings = PodcastHomeUndoStrings(
                undoLabel = "Undo",
                removedFromInbox = "Removed from Inbox",
                episodesRemovedFromInbox = { "$it episodes removed from Inbox" },
                addToNextFailed = "Could not add to Up next",
                addedToNext = "Added to Up next",
                removeFromNextFailed = "Could not remove from Up next",
                unfollowed = "Unfollowed",
                downloadRemoved = "Download removed",
                removeDownloadFailed = "Could not remove download",
            ),
            addToQueueFromInbox = { _, _, _ -> },
            undoAddToQueueFromInbox = { Result.success(Unit) },
            dismissFromInbox = {},
            restoreToInbox = {},
        )
    }

    private class PendingUndoSnackbarPresenter : UndoSnackbarPresenter {
        private var result: CompletableDeferred<Boolean>? = null

        override suspend fun present(offer: UndoOffer): Boolean =
            CompletableDeferred<Boolean>().also { result = it }.await()

        fun resolve(undo: Boolean) {
            result?.complete(undo)
        }
    }

    @Test
    fun aRowThatComesBackAfterAnUndoIsNotDismissedASecondTime() {
        val dismissed = mutableListOf<Long>()
        val inInbox = mutableStateOf(true)

        composeRule.setContent {
            BoothAppTheme {
                // The dismissal takes the row out of the list, the way the Inbox query does, so the row's
                // composition is disposed while it is still dismissed. Putting it back is what an undo
                // does, and because Material3 saves the dismiss value it returns already dismissed and
                // re-announces itself to onDismiss with no gesture behind it.
                val inbox = if (inInbox.value) listOf(episode(1L)) else emptyList()
                LazyColumn {
                    items(inbox, key = { it.id }) { row ->
                        InboxEpisodeSwipeRow(
                            episode = row,
                            podcastTitle = "Test podcast",
                            active = false,
                            selected = false,
                            selectionMode = false,
                            onOpen = {},
                            onAddToQueue = {},
                            onDismiss = {
                                dismissed += row.id
                                inInbox.value = false
                            },
                            onLongPress = {},
                            onActions = {},
                            onToggleSelection = {},
                            onPlay = {},
                            onDownload = {},
                            isPlaying = false,
                            isBuffering = false,
                            positionOverride = null,
                            downloadProgress = null,
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Episode 1").assertIsDisplayed()

        swipeRowLeft()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Episode 1").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(listOf(1L), dismissed) }

        // Undo: the row comes back. This is the regression. It used to announce the same dismiss again
        // and remove the episode a second time, so the episode the undo restored was immediately
        // removed again and the undo looked like it had done nothing.
        composeRule.runOnIdle {
            dismissed.clear()
            inInbox.value = true
        }
        composeRule.waitForIdle()

        // Asserted as *displayed*, not merely present, because that is the only assertion that can
        // see the other half of this failure: the row returning carrying its dismissed offset. In that
        // state everything else still passes — the undo ran, the row is back in the list, it simply
        // refuses to be swiped again — but the row is translated off screen, its clipped bounding box
        // is outside the window, and this fails.
        composeRule.onNodeWithText("Episode 1").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList<Long>(), dismissed) }

        // And the guard that blocked the repeat re-armed, so the row is not dead to the finger.
        swipeRowLeft()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(listOf(1L), dismissed) }
    }

    @Test
    fun swipingTheMiniPlayerOffStopsItAndTheOverlayIsSwipeableAgain() {
        var dismissed = 0

        composeRule.setContent {
            BoothAppTheme {
                PodcastHomeMiniPlayerOverlay(
                    visible = true,
                    episode = episode(1L),
                    podcastTitle = "Test podcast",
                    isPlaying = true,
                    isBuffering = false,
                    onTogglePlayPause = {},
                    onClearRememberedEpisode = {},
                    onStopAndClear = {},
                    onOpen = {},
                    onDismiss = { dismissed++ },
                    onHeightChange = {},
                )
            }
        }

        composeRule.onNodeWithTag(SWIPE_ROW_TEST_TAG).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(1, dismissed) }

        // Reset is what makes the second swipe possible at all: a player left dismissed is a player
        // that cannot be swiped again, and looks perfectly normal while it is.
        composeRule.onNodeWithTag(SWIPE_ROW_TEST_TAG).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(2, dismissed) }
    }

    @Test
    fun swipingARowAlwaysActsOnThatRowAndNotOnItsNeighbours() {
        // A removed row makes every row below it move up a slot. If anything a row holds is keyed by
        // slot rather than by item, it arrives at the wrong row: the swipe then either does nothing,
        // because it inherits a neighbour's "already acted on", or reports the wrong episode. That is
        // the shape of the reported bug, where swiping the top row did nothing.
        val inInbox = mutableStateOf(listOf(1L, 2L, 3L))
        val dismissed = mutableListOf<Long>()

        composeRule.setContent {
            BoothAppTheme {
                LazyColumn {
                    items(inInbox.value, key = { "inbox-$it" }) { id ->
                        InboxEpisodeSwipeRow(
                            episode = episode(id),
                            podcastTitle = "Test podcast",
                            active = false,
                            selected = false,
                            selectionMode = false,
                            onOpen = {},
                            onAddToQueue = {},
                            onDismiss = { dismissed += id },
                            onLongPress = {},
                            onActions = {},
                            onToggleSelection = {},
                            onPlay = {},
                            onDownload = {},
                            isPlaying = false,
                            isBuffering = false,
                            positionOverride = null,
                            downloadProgress = null,
                        )
                    }
                }
            }
        }

        fun swipeRowAt(index: Int) {
            composeRule.onAllNodesWithTag(SWIPE_ROW_TEST_TAG)[index]
                .performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
        }

        // Every row in turn, each swipe followed by a removal and an undo-style reinsertion, so each
        // one happens with a different set of rows shifted up into the slots below it.
        for (expected in listOf(1L, 2L, 3L, 1L, 3L, 2L, 1L)) {
            dismissed.clear()
            swipeRowAt(inInbox.value.indexOf(expected))
            composeRule.runOnIdle { assertEquals("row $expected must act on itself", listOf(expected), dismissed) }

            // Undo: the row leaves the list and comes straight back.
            composeRule.runOnIdle { inInbox.value = inInbox.value - expected }
            composeRule.waitForIdle()
            dismissed.clear()
            composeRule.runOnIdle { inInbox.value = (inInbox.value + expected).sorted() }
            composeRule.waitForIdle()

            // Coming back must not dismiss itself.
            composeRule.runOnIdle {
                assertEquals("returning row $expected must not re-dismiss", emptyList<Long>(), dismissed)
            }
        }
    }

    /**
     * Swipes the row under test across enough of its width to pass the 50% positional threshold.
     *
     * Targets the row's own tag rather than any text inside it. The rows disagree about where the
     * podcast title lives — text on the subscriptions card, a contentDescription on the Inbox row,
     * and no mention of it at all on a downloads row — so a text-based target found a node in one
     * test and failed to find one in the rest, with an error that never mentioned the swipe.
     */
    private fun swipeRowLeft() {
        composeRule.onNodeWithTag(SWIPE_ROW_TEST_TAG)
            .performTouchInput { swipeLeft() }
    }

    private fun swipeRowRight() {
        composeRule.onNodeWithTag(SWIPE_ROW_TEST_TAG)
            .performTouchInput { swipeRight() }
    }

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

    private fun asset(episodeId: Long) = DownloadAssetEntity(
        episodeId = episodeId,
        assetType = DownloadAssetType.AUDIO,
        downloadId = 1L,
        sourceUrl = "https://example.com/$episodeId.mp3",
        destinationUri = "file:///tmp/$episodeId.mp3",
        status = DownloadAssetStatus.COMPLETED,
        bytesDownloaded = 1_000L,
        totalBytes = 1_000L,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
    )
}
