package com.shapeshed.booth.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.Episode
import com.shapeshed.booth.data.PodcastDiscoveryCategory
import com.shapeshed.booth.data.PodcastSearchResult

/** Adapts home state and route events to the discovery screen. */
@Composable
internal fun PodcastHomeDiscoveryDestination(
    homeUiState: PodcastHomeUiState,
    viewModel: PodcastViewModel,
    playbackViewModel: PodcastPlaybackViewModel,
    context: Context,
    queueEpisodeIds: List<Long>,
    subscribedFeedUrls: Set<String>,
    twoPane: Boolean,
    onSubscribe: (PodcastSearchResult) -> Unit,
    onUnsubscribe: () -> Unit,
    onShowDescription: () -> Unit,
    onShowNowPlayingChange: (Boolean) -> Unit,
    onEpisodeAction: (PodcastEpisodeAction) -> Unit,
    onQueueError: () -> Unit,
    onOpenPodcast: () -> Unit,
    onCategory: (PodcastDiscoveryCategory) -> Unit,
    onEpisode: (Episode) -> Unit,
    modifier: Modifier = Modifier,
) {
    PodcastDiscoveryContent(
        state = homeUiState.homeState,
        previewEpisodeEntities = homeUiState.previewEpisodeEntities,
        playback = homeUiState.playback,
        viewModel = viewModel,
        playbackViewModel = playbackViewModel,
        context = context,
        downloadProgress = homeUiState.downloadProgress,
        queueEpisodeIds = queueEpisodeIds,
        subscribedFeedUrls = subscribedFeedUrls,
        onSubscribe = onSubscribe,
        onUnsubscribe = onUnsubscribe,
        onShowDescription = onShowDescription,
        onShowNowPlayingChange = onShowNowPlayingChange,
        onEpisodeAction = onEpisodeAction,
        onQueueError = onQueueError,
        onOpenPodcast = onOpenPodcast,
        onCategory = onCategory,
        onEpisode = onEpisode,
        modifier = modifier,
        twoPane = twoPane,
    )
}
