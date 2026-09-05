package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity

@Composable
internal fun PodcastEpisodeContent(
    episode: EpisodeEntity,
    podcastTitle: String,
    podcastArtworkUrl: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlay: () -> Unit,
    onWatch: (() -> Unit)?,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    downloadProgress: DownloadProgress?,
    isInQueue: Boolean,
    onToggleQueue: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenPodcast: () -> Unit,
    isSubscribed: Boolean,
    isSubscriptionLoading: Boolean = false,
    onSubscribe: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    twoPane: Boolean = false,
) {
    PodcastEpisodeDetail(
        episode = episode,
        podcastTitle = podcastTitle,
        podcastArtworkUrl = podcastArtworkUrl,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        onPlay = onPlay,
        onWatch = onWatch,
        onDownload = onDownload,
        onRemoveDownload = onRemoveDownload,
        downloadProgress = downloadProgress,
        isInQueue = isInQueue,
        onToggleQueue = onToggleQueue,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onOpenPodcast = onOpenPodcast,
        isSubscribed = isSubscribed,
        isSubscriptionLoading = isSubscriptionLoading,
        onSubscribe = onSubscribe,
        twoPane = twoPane,
        modifier = modifier,
    )
}
