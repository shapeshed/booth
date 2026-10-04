package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.DownloadAssetType
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.ui.theme.BoothAppTheme
import com.shapeshed.booth.ui.theme.Spacing

/**
 * Visual cover for the episode detail page, an episode row and a podcast header.
 *
 * These exist because spacing and typography changes are exactly the kind that pass every behavioural
 * test and still look wrong. The reference images are the review artefact: they show whether the
 * header stack, the section heading and the title hierarchy actually read as intended, which no
 * assertion about `spacedBy` values can express.
 *
 * Rendered at a phone width, so this is the one-pane branch. The two-pane branch is covered by
 * `BoothAdaptiveScreenshotTest`.
 */
private const val LONG_DESCRIPTION =
    "<p>The FT's Katie Martin and Rob Armstrong discuss what a slowing AI trade means for investors, " +
        "and why the market has been so willing to look through it.</p>" +
        "<h2>Why it matters</h2>" +
        "<p>Slowing down could reduce the enormous amounts of cash being lavished on training new " +
        "models, which changes the calculus for everyone selling into that spend.</p>" +
        "<ul><li>Data centre spending</li><li>Power constraints</li></ul>"

@PreviewTest
@Preview(name = "Episode detail", device = "spec:width=400dp,height=900dp,dpi=420")
@Composable
private fun EpisodeDetailScreenshot() {
    BoothAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            PodcastEpisodeDetailContent(
                artworkUrl = null,
                title = "Unhedged: An ode to stock picking",
                podcastTitle = "FT News Briefing",
                podcastArtworkUrl = null,
                descriptionHtml = LONG_DESCRIPTION,
                audioSizeBytes = 29_200_000L,
                videoSizeBytes = null,
                durationMs = 21L * 60L * 1_000L,
                positionMs = 0L,
                completed = false,
                publishedAtMillis = 1_700_000_000_000L,
                explicit = false,
                isSubscribed = true,
                onSubscription = {},
                isPlaying = false,
                isBuffering = false,
                onPlay = {},
                onWatch = null,
                onDownload = {},
                onRemoveDownload = {},
                downloadProgress = null,
                isInQueue = false,
                onToggleQueue = {},
                isDownloaded = false,
                isFavorite = false,
                onToggleFavorite = {},
                onOpenPodcast = {},
            )
        }
    }
}

/**
 * The four download states on one screen, so the badge's slot can be compared across them.
 */
@PreviewTest
@Preview(name = "Episode row states", device = "spec:width=400dp,height=460dp,dpi=420")
@Composable
private fun EpisodeRowStatesScreenshot() {
    BoothAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(Spacing.screenInset),
                // Spaced like a real list, so the rows are distinguishable and the badges can be
                // compared without the date of one row reading as the subtitle of the one above.
                verticalArrangement = Arrangement.spacedBy(Spacing.block),
            ) {
                EpisodeTitleBlock(
                    episode = previewEpisode(),
                    active = false,
                )
                EpisodeTitleBlock(
                    episode = previewEpisode(),
                    active = false,
                    downloadAsset = previewAsset(DownloadAssetStatus.DOWNLOADING),
                )
                EpisodeTitleBlock(
                    episode = previewEpisode(),
                    active = false,
                    downloadAsset = previewAsset(DownloadAssetStatus.FAILED),
                )
                EpisodeTitleBlock(
                    episode = previewEpisode(),
                    active = false,
                    downloadAsset = previewAsset(DownloadAssetStatus.COMPLETED),
                    downloaded = true,
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Podcast header", device = "spec:width=400dp,height=340dp,dpi=420")
@Composable
private fun PodcastHeaderScreenshot() {
    BoothAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            PodcastDetailHeader(
                title = "FT News Briefing",
                artworkUrl = null,
                author = "Financial Times",
                isSubscribed = true,
                onSubscription = {},
                latestAction = { actionModifier -> Text("Follow", modifier = actionModifier) },
                onArtworkClick = {},
                description = "A rundown of the most important global business stories you need to know.",
            )
        }
    }
}

private fun previewEpisode() = EpisodeEntity(
    id = 1L,
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
    localUri = null,
    inInbox = true,
    firstSeenAtMillis = null,
)

private fun previewAsset(status: DownloadAssetStatus) = DownloadAssetEntity(
    episodeId = 1L,
    assetType = DownloadAssetType.AUDIO,
    downloadId = 1L,
    sourceUrl = "https://example.com/1.mp3",
    destinationUri = "file:///tmp/1.mp3",
    status = status,
    bytesDownloaded = 12_000_000L,
    totalBytes = 29_200_000L,
    errorMessage = "The audio file is no longer available at this address.",
    createdAtMillis = 0L,
    updatedAtMillis = 0L,
)
