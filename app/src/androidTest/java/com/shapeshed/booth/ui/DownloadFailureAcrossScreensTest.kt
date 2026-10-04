package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.booth.data.DOWNLOAD_UNAVAILABLE_MESSAGE
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.DownloadAssetType
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.MAX_DOWNLOAD_RETRIES
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.ui.theme.BoothAppTheme
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The same failed episode, rendered on all three list views.
 *
 * The reported bug was not that one screen drew the icon oddly. It was that the three screens
 * disagreed: Downloads drew a failure indicator inline in the metadata line, while Up next and Inbox
 * drew nothing at all, so the same failed episode looked broken in one list and untouched in the
 * others. Asserting per screen would let that pass, because each was individually defensible.
 * Asserting that all three agree is the property that was actually missing, so that is what these do.
 */
class DownloadFailureAcrossScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val failedStatus = DownloadAssetStatus.FAILED

    @Test
    fun theFailedBadgeSitsInTheSameSlotOnAllThreeScreens() {
        composeRule.setContent {
            BoothAppTheme {
                Column {
                    UpNextRow()
                    InboxRow()
                    DownloadsRow()
                }
            }
        }
        composeRule.waitForIdle()

        // Three badges, one per screen. Before the change Up next and Inbox rendered none at all.
        val badges = composeRule.onAllNodesWithContentDescription("Download failed", useUnmergedTree = true)
        badges.assertCountEquals(3)
        val heights = badges.fetchSemanticsNodes().map { it.boundsInRoot.height.roundToInt() }

        assertEquals(
            "badge height is not identical across the three screens: $heights",
            1,
            heights.distinct().size,
        )
    }

    @Test
    fun everyScreenLabelsTheFailureForScreenReaders() {
        composeRule.setContent {
            BoothAppTheme {
                Column {
                    UpNextRow()
                    InboxRow()
                    DownloadsRow()
                }
            }
        }
        // Three badges, one per screen: before the change Up next and Inbox rendered none.
        composeRule.onAllNodesWithContentDescription("Download failed", useUnmergedTree = true)
            .assertCountEquals(3)
    }

    /**
     * The Downloads row carries no metadata line at all.
     *
     * The status badge has to share the title block's one trailing slot with the offline tick, and the
     * size used to sit under the title beside it. That second line is precisely what pushed a failed
     * row's indicator down out of line with every other row, so it is gone from the row and lives in
     * the dialog instead.
     */
    @Test
    fun theDownloadsRowHasNoSizeOrStatusLineUnderTheTitle() {
        composeRule.setContent {
            BoothAppTheme {
                Column {
                    DownloadsRow()
                }
            }
        }
        // With a single 29.2 MB download the header total is also "29.2 MB", so counting the string
        // cannot distinguish the header from a per-row line. Checking where the one occurrence sits
        // can: the header is above the episode, and a row-level line would be below its title.
        val sizes = composeRule.onAllNodesWithText("29.2 MB").fetchSemanticsNodes()
        assertEquals("expected only the header total", 1, sizes.size)
        val titleTop = composeRule.onNodeWithText("Unhedged: An ode to stock picking")
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            "the size is still drawn on the row, below its title",
            sizes.single().boundsInRoot.bottom < titleTop,
        )
        composeRule.onAllNodesWithContentDescription("Download failed", useUnmergedTree = true)
            .assertCountEquals(1)
    }

    /**
     * The reason is what the user acts on, so it moves into the dialog rather than the row.
     */
    @Test
    fun theFailureDialogCarriesTheSizeAndTheRecordedReason() {
        composeRule.setContent {
            BoothAppTheme {
                DownloadFailureDialog(
                    episodeTitle = "Unhedged: An ode to stock picking",
                    asset = failedAsset(),
                    onRetry = {},
                    onRemove = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("Size: 29.2 MB").assertIsDisplayed()
        composeRule.onNodeWithText("The server connection failed while downloading.")
            .assertIsDisplayed()
    }

    /**
     * The real failure on the developer's device: the enclosure URL 404s, so it is named as gone
     * rather than left as a raw code, and retrying is discouraged.
     */
    @Test
    fun aMissingEnclosureIsExplainedRatherThanShownAsACode() {
        composeRule.setContent {
            BoothAppTheme {
                DownloadFailureDialog(
                    episodeTitle = "Unhedged: An ode to stock picking",
                    // The exact value the receiver stores for an HTTP 404, so this pins the contract
                    // between the error mapping and the dialog rather than a hand-invented string.
                    asset = failedAsset(errorMessage = DOWNLOAD_UNAVAILABLE_MESSAGE),
                    onRetry = {},
                    onRemove = {},
                    onDismiss = {},
                )
            }
        }
        // The recorded reason, which the error mapping has already turned into the "gone" wording.
        composeRule.onNodeWithText(DOWNLOAD_UNAVAILABLE_MESSAGE).assertIsDisplayed()
        // And the advice changes: a gone enclosure is not advertised as retryable.
        composeRule.onNodeWithText(
            "This episode's audio is no longer published. Removing the download clears this.",
        ).assertIsDisplayed()
        // The retry budget line must not appear for a failure a retry cannot fix.
        composeRule.onAllNodesWithText(
            "Booth retries a failed download up to $MAX_DOWNLOAD_RETRIES times. " +
                "Retrying starts a fresh set of attempts.",
        ).assertCountEquals(0)
    }

    /**
     * Driven through the real screen rather than a counter passed in.
     *
     * An earlier version of this test handed `DownloadsRow` an `onShowFailure` lambda and asserted it
     * fired. It could never fire: the screen opens the dialog from its own state, so the composable's
     * parameter was unused and the assertion was measuring nothing. Tapping the real badge and
     * expecting the real dialog is the version that can fail.
     */
    @Test
    fun tappingTheBadgeOpensTheExplanationFromTheDownloadsScreen() {
        composeRule.setContent {
            BoothAppTheme {
                Column {
                    DownloadsRow()
                }
            }
        }
        composeRule.onNodeWithContentDescription("Download failed").performClick()
        composeRule.onNodeWithText("Download failed").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun theDownloadsScreenOffersARetryThatReachesTheAction() {
        var retried = 0L
        composeRule.setContent {
            BoothAppTheme {
                val scope = rememberCoroutineScope()
                val pending = remember { mutableStateOf(emptySet<Long>()) }
                PodcastDownloadsScreen(
                    assets = listOf(failedAsset()),
                    episodes = mapOf(EPISODE_ID to episode(downloaded = false)),
                    podcastsById = mapOf(1L to podcast()),
                    activeEpisodeId = null,
                    isPlaying = false,
                    isBuffering = false,
                    downloadProgress = emptyMap(),
                    onOpen = {},
                    onPlay = {},
                    onDownload = {},
                    onRemove = { Result.success(Unit) },
                    onLongPress = {},
                    undoActions = rememberTestDownloadUndoActions(scope),
                    pendingRemovalEpisodeIds = pending.value,
                    onPendingRemovalEpisodeChange = { _, _ -> },
                    onRetryDownload = { retried++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Download failed").performClick()
        composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(EPISODE_ID, retried) }
    }

    @Test
    fun aCompletedDownloadStillShowsTheOfflineBadgeOnEveryScreen() {
        composeRule.setContent {
            BoothAppTheme {
                Column {
                    UpNextRow(downloaded = true, status = DownloadAssetStatus.COMPLETED)
                    InboxRow(downloaded = true, status = DownloadAssetStatus.COMPLETED)
                    DownloadsRow(downloaded = true, status = DownloadAssetStatus.COMPLETED)
                }
            }
        }
        composeRule.onAllNodesWithContentDescription("Available offline", useUnmergedTree = true)
            .assertCountEquals(3)
    }

    @Composable
    private fun UpNextRow(
        downloaded: Boolean = false,
        status: DownloadAssetStatus = failedStatus,
        onShowFailure: () -> Unit = {},
    ) {
        QueueEpisodeSwipeRow(
            episode = episode(downloaded),
            podcastTitle = "FT News Briefing",
            active = false,
            dragging = false,
            progressOverride = null,
            positionOverride = null,
            isPlaying = false,
            isBuffering = false,
            downloadProgress = null,
            downloadAsset = asset(status),
            onOpen = {},
            onPlay = {},
            onDownload = {},
            onLongPress = {},
            onRemove = {},
            onShowDownloadFailure = onShowFailure,
            showDragHandle = false,
        )
    }

    @Composable
    private fun InboxRow(
        downloaded: Boolean = false,
        status: DownloadAssetStatus = failedStatus,
        onShowFailure: () -> Unit = {},
    ) {
        InboxEpisodeSwipeRow(
            episode = episode(downloaded),
            podcastTitle = "FT News Briefing",
            active = false,
            selected = false,
            selectionMode = false,
            onOpen = {},
            onAddToQueue = {},
            onDismiss = {},
            onLongPress = {},
            onActions = {},
            onToggleSelection = {},
            onPlay = {},
            onDownload = {},
            isPlaying = false,
            isBuffering = false,
            positionOverride = null,
            downloadProgress = null,
            downloadAsset = asset(status),
            onShowDownloadFailure = onShowFailure,
        )
    }

    @Composable
    private fun DownloadsRow(downloaded: Boolean = false, status: DownloadAssetStatus = failedStatus) {
        val scope = rememberCoroutineScope()
        val pending = remember { mutableStateOf(emptySet<Long>()) }
        PodcastDownloadsScreen(
            assets = listOf(asset(status)),
            episodes = mapOf(EPISODE_ID to episode(downloaded)),
            podcastsById = mapOf(1L to podcast()),
            activeEpisodeId = null,
            isPlaying = false,
            isBuffering = false,
            downloadProgress = emptyMap(),
            onOpen = {},
            onPlay = {},
            onDownload = {},
            onRemove = { Result.success(Unit) },
            onLongPress = {},
            undoActions = rememberTestDownloadUndoActions(scope),
            pendingRemovalEpisodeIds = pending.value,
            onPendingRemovalEpisodeChange = { _, _ -> },
        )
    }

    private fun podcast() = PodcastEntity(
        id = 1L,
        title = "FT News Briefing",
        author = "Financial Times",
        feedUrl = "https://example.com/feed.xml",
        artworkUrl = null,
        siteUrl = null,
        descriptionHtml = null,
        subscribedAtMillis = 0L,
        lastRefreshMillis = null,
    )

    private fun episode(downloaded: Boolean) = EpisodeEntity(
        id = EPISODE_ID,
        podcastId = 1L,
        guid = "episode-1",
        title = "Unhedged: An ode to stock picking",
        descriptionHtml = null,
        audioUrl = "https://example.com/1.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = 1_700_000_000_000L,
        durationMs = 21L * 60L * 1_000L,
        positionMs = 0L,
        completed = false,
        localUri = if (downloaded) "file:///tmp/1.mp3" else null,
        inInbox = true,
        firstSeenAtMillis = null,
    )

    private fun asset(status: DownloadAssetStatus) = DownloadAssetEntity(
        episodeId = EPISODE_ID,
        assetType = DownloadAssetType.AUDIO,
        downloadId = 1L,
        sourceUrl = "https://example.com/1.mp3",
        destinationUri = "file:///tmp/1.mp3",
        status = status,
        bytesDownloaded = 29_200_000L,
        totalBytes = 29_200_000L,
        errorMessage = "The server connection failed while downloading.",
        retryCount = MAX_DOWNLOAD_RETRIES,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    private fun failedAsset(errorMessage: String? = "The server connection failed while downloading.") =
        asset(DownloadAssetStatus.FAILED).copy(errorMessage = errorMessage)

    private companion object {
        const val EPISODE_ID = 1L
    }
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
