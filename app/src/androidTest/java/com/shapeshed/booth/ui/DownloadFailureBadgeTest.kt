package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.DownloadAssetType
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.MAX_DOWNLOAD_RETRIES
import com.shapeshed.booth.ui.theme.BoothAppTheme
import kotlin.math.round
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The reported bug: a failed download drew its icon somewhere different on every list.
 *
 * On Downloads it sat inline in the metadata line next to the file size, at a different height and a
 * different x from the tick a downloaded episode gets. In Up next and Inbox it was not drawn at all,
 * so a failed episode looked exactly like one that had never been downloaded. Asserting on content
 * descriptions alone would not have caught the position, so these tests compare where the badge sits
 * relative to the downloaded case, which is the thing that actually differed.
 */
class DownloadFailureBadgeTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * The core regression: both badges must land in the same slot of the title block.
     *
     * Both rows are rendered side by side in one composition, because the Compose test rule allows
     * `setContent` only once per test. Rendering them separately and comparing coordinates would have
     * compared two different layout passes rather than the two states.
     */
    @Test
    fun aFailedDownloadShowsABadgeInTheSameSlotAsADownloadedOne() {
        var downloadedBadge: BadgeBox? = null
        var failedBadge: BadgeBox? = null

        composeRule.setContent {
            BoothAppTheme {
                Column {
                    QueueEpisodeSwipeRow(
                        episode = episode(downloaded = true, title = DOWNLOADED_TITLE),
                        podcastTitle = "FT News Briefing",
                        active = false,
                        dragging = false,
                        progressOverride = null,
                        positionOverride = null,
                        isPlaying = false,
                        isBuffering = false,
                        downloadProgress = null,
                        downloadAsset = asset(DownloadAssetStatus.COMPLETED, episodeId = 1L),
                        onOpen = {},
                        onPlay = {},
                        onDownload = {},
                        onLongPress = {},
                        onRemove = {},
                        showDragHandle = false,
                    )
                    QueueEpisodeSwipeRow(
                        episode = episode(downloaded = false, title = FAILED_TITLE),
                        podcastTitle = "FT News Briefing",
                        active = false,
                        dragging = false,
                        progressOverride = null,
                        positionOverride = null,
                        isPlaying = false,
                        isBuffering = false,
                        downloadProgress = null,
                        downloadAsset = asset(DownloadAssetStatus.FAILED, episodeId = 2L),
                        onOpen = {},
                        onPlay = {},
                        onDownload = {},
                        onLongPress = {},
                        onRemove = {},
                        showDragHandle = false,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Measured relative to each row's own title, because the two rows sit at different
        // absolute offsets in the column. What has to match is the badge's slot within its row.
        val downloaded = badgeOffsetWithinRow("Available offline", DOWNLOADED_TITLE)
        val failed = badgeOffsetWithinRow("Download failed", FAILED_TITLE)

        assertEquals("badge height drifted", downloaded.height, failed.height)
        assertEquals(
            "failed badge sits at a different height within its row than the downloaded badge",
            downloaded.offsetFromTitleTop,
            failed.offsetFromTitleTop,
        )
        assertEquals(
            "failed badge is not flush with the row's trailing edge",
            downloaded.offsetFromTitleRight,
            failed.offsetFromTitleRight,
        )
    }

    @Test
    fun aFailedDownloadIsLabelledForScreenReaders() {
        setRowContent(DownloadAssetStatus.FAILED, downloaded = false)
        composeRule.onNodeWithContentDescription("Download failed").assertIsDisplayed()
    }

    @Test
    fun aRetryingDownloadIsNoLongerInvisible() {
        // The old code returned early for RETRYING, so a retrying download drew nothing at all while
        // the sort order still treated it as in progress.
        setRowContent(DownloadAssetStatus.RETRYING, downloaded = false)
        composeRule.onNodeWithContentDescription("Retrying").assertIsDisplayed()
    }

    @Test
    fun tappingTheFailedBadgeOpensTheExplanation() {
        var shown = 0
        setRowContent(DownloadAssetStatus.FAILED, downloaded = false, onShowFailure = { shown++ })

        composeRule.onNodeWithContentDescription("Download failed").performClick()

        composeRule.runOnIdle { assertEquals(1, shown) }
    }

    @Test
    fun theDownloadedBadgeDoesNotOpenTheFailureExplanation() {
        var shown = 0
        setRowContent(DownloadAssetStatus.COMPLETED, downloaded = true, onShowFailure = { shown++ })

        composeRule.onNodeWithContentDescription("Available offline").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, shown) }
    }

    @Test
    fun theDialogExplainsTheFailureAndOffersARetry() {
        var retried = 0L
        var removed = 0L
        composeRule.setContent {
            BoothAppTheme {
                DownloadFailureDialog(
                    episodeTitle = "Unhedged: An ode to stock picking",
                    asset = failedAsset(retryCount = MAX_DOWNLOAD_RETRIES),
                    onRetry = { retried++ },
                    onRemove = { removed++ },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Download failed").assertIsDisplayed()
        composeRule.onNodeWithText("Unhedged: An ode to stock picking").assertIsDisplayed()
        // The recorded reason, which is Booth's own wording rather than a platform code.
        composeRule.onNodeWithText("The server connection failed while downloading.").assertIsDisplayed()
        // The retry budget is quoted, so a person can tell a dead URL from an unlucky one.
        composeRule.onNodeWithText(
            "Booth retries a failed download up to $MAX_DOWNLOAD_RETRIES times. " +
                "Retrying starts a fresh set of attempts.",
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle {
            assertEquals(1L, retried)
            assertEquals(0L, removed)
        }
    }

    @Test
    fun theDialogCanRemoveTheFailedDownloadInstead() {
        var retried = 0L
        var removed = 0L
        composeRule.setContent {
            BoothAppTheme {
                DownloadFailureDialog(
                    episodeTitle = "Episode 1",
                    asset = failedAsset(retryCount = MAX_DOWNLOAD_RETRIES),
                    onRetry = { retried++ },
                    onRemove = { removed++ },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Remove download").performClick()
        composeRule.runOnIdle {
            assertEquals(1L, removed)
            assertEquals(0L, retried)
        }
    }

    /**
     * A row whose failure reason was never recorded must still say something, rather than showing an
     * empty dialog body.
     */
    @Test
    fun theDialogFallsBackWhenNoReasonWasRecorded() {
        composeRule.setContent {
            BoothAppTheme {
                DownloadFailureDialog(
                    episodeTitle = "Episode 1",
                    asset = failedAsset(retryCount = 0, errorMessage = null),
                    onRetry = {},
                    onRemove = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("The reason for the failure was not recorded.").assertIsDisplayed()
    }

    @Test
    fun aCancelledDownloadIsExplainedAsCancelledRatherThanFailed() {
        composeRule.setContent {
            BoothAppTheme {
                DownloadFailureDialog(
                    episodeTitle = "Episode 1",
                    asset = failedAsset(status = DownloadAssetStatus.CANCELLED),
                    onRetry = {},
                    onRemove = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Download cancelled").assertIsDisplayed()
    }

    private fun setRowContent(status: DownloadAssetStatus, downloaded: Boolean, onShowFailure: (() -> Unit)? = null) {
        composeRule.setContent {
            BoothAppTheme {
                QueueEpisodeSwipeRow(
                    episode = episode(downloaded = downloaded),
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
        }
    }

    /**
     * The icon must actually paint.
     *
     * This exists because of a bug a semantics-only assertion could not see: the badge's modifier
     * chain was `size(16.dp).padding(8.dp)`, and padding applied inside a fixed size leaves a 0x0
     * content box. The node was laid out and reported a content description at the right coordinates,
     * so `uiautomator` and `fetchSemanticsNode().boundsInRoot` both looked perfect, while the screen
     * showed nothing at all.
     *
     * `assertIsDisplayed` checks that a node exists and is within the window, not that its pixels are
     * non-empty, so it passes for a zero-sized icon. The only reliable signal available without
     * screenshot comparison is the measured size, which is why this asserts height as well.
     */
    @Test
    fun theFailureBadgeIsActuallyVisibleAndNotCollapsed() {
        setRowContent(DownloadAssetStatus.FAILED, downloaded = false)
        val node = composeRule.onNodeWithContentDescription("Download failed")
            .fetchSemanticsNode()
        val bounds = node.boundsInRoot
        assertTrue(
            "failure badge collapsed to zero width, so nothing is drawn: $bounds",
            bounds.width > 1f,
        )
        assertTrue(
            "failure badge collapsed to zero height, so nothing is drawn: $bounds",
            bounds.height > 1f,
        )
        // Comfortably tappable, since a failure needs to be reachable.
        assertTrue(
            "failure badge is too small to tap reliably: $bounds",
            bounds.width >= 24f && bounds.height >= 24f,
        )
    }

    @Test
    fun theOfflineBadgeIsNotCollapsedEither() {
        setRowContent(DownloadAssetStatus.COMPLETED, downloaded = true)
        val bounds = composeRule.onNodeWithContentDescription("Available offline")
            .fetchSemanticsNode().boundsInRoot
        assertTrue("offline badge collapsed: $bounds", bounds.width > 1f && bounds.height > 1f)
    }

    /**
     * A failed download has no local file, but the episode still streams. The row used to gate the
     * play control on `isDownloaded`, which left a failed row looking unplayable.
     */
    @Test
    fun aFailedDownloadStillOffersStreamingPlayback() {
        setRowContent(DownloadAssetStatus.FAILED, downloaded = false)
        composeRule.onNodeWithContentDescription("Play episode").assertIsDisplayed()
    }

    /**
     * Where a badge sits within its own row: its size, and its offset from the row's title text.
     *
     * Absolute root coordinates are not comparable between two rows stacked in one column, so the
     * offsets are taken against each row's own title. That is also the more meaningful quantity: it
     * is "which slot of the row does the badge occupy", which is exactly what differed between the
     * three screens before this change.
     */
    private data class BadgeBox(val height: Int, val offsetFromTitleTop: Int, val offsetFromTitleRight: Int)

    private fun badgeOffsetWithinRow(badgeDescription: String, titleText: String): BadgeBox {
        val badge = composeRule.onNodeWithContentDescription(badgeDescription)
            .fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithText(titleText)
            .fetchSemanticsNode().boundsInRoot
        return BadgeBox(
            height = badge.height.roundToInt(),
            offsetFromTitleTop = (badge.top - title.top).roundToInt(),
            offsetFromTitleRight = (badge.right - title.right).roundToInt(),
        )
    }

    private companion object {
        // Distinct titles so each row's badge can be measured against its own title node.
        const val DOWNLOADED_TITLE = "Downloaded episode"
        const val FAILED_TITLE = "Failed episode"
    }

    private fun episode(downloaded: Boolean, title: String = "Unhedged: An ode to stock picking") = EpisodeEntity(
        id = if (downloaded) 1L else 2L,
        podcastId = 1L,
        guid = "episode-$title",
        title = title,
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

    private fun asset(status: DownloadAssetStatus, episodeId: Long = 1L) = DownloadAssetEntity(
        episodeId = episodeId,
        assetType = DownloadAssetType.AUDIO,
        downloadId = episodeId,
        sourceUrl = "https://example.com/$episodeId.mp3",
        destinationUri = "file:///tmp/$episodeId.mp3",
        status = status,
        bytesDownloaded = 29_200_000L,
        totalBytes = 29_200_000L,
        errorMessage = "The server connection failed while downloading.",
        retryCount = MAX_DOWNLOAD_RETRIES,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    private fun failedAsset(
        status: DownloadAssetStatus = DownloadAssetStatus.FAILED,
        retryCount: Int = 0,
        errorMessage: String? = "The server connection failed while downloading.",
    ) = asset(status).copy(retryCount = retryCount, errorMessage = errorMessage)
}

private fun Float.roundToInt(): Int = round(this).toInt()
