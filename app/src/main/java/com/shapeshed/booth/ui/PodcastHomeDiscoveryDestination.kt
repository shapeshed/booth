package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.booth.data.Episode
import com.shapeshed.booth.data.PodcastDiscoveryCategory
import com.shapeshed.booth.data.PodcastSearchResult

/**
 * Adapts home state and route events to the discovery screen.
 *
 * Takes the actions rather than the ViewModels, and the played position rather than collecting it
 * itself.
 */
@Composable
internal fun PodcastHomeDiscoveryDestination(
    homeUiState: PodcastHomeUiState,
    actions: PodcastDiscoveryActions,
    playbackProgress: PlaybackProgress,
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
        playbackProgress = playbackProgress,
        actions = actions,
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

/**
 * Collects the played position for the discovery subtree only.
 *
 * This exists purely to keep the 2 Hz collection scoped. Hoisting the collection into the home
 * screen body would recompose the whole screen twice a second, which is what splitting
 * PlaybackProgress out of PlaybackUiState was for. The same reasoning already applies to the
 * per-page collection in the podcast detail pager, and this is that pattern applied here: a thin
 * wrapper that collects and immediately delegates, so nothing above it sees the ticks.
 */
@Composable
internal fun ScopedPodcastHomeDiscoveryDestination(
    homeUiState: PodcastHomeUiState,
    actions: PodcastDiscoveryActions,
    playbackViewModel: PodcastPlaybackViewModel,
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
    val progress by playbackViewModel.progress.collectAsStateWithLifecycle()
    PodcastHomeDiscoveryDestination(
        homeUiState = homeUiState,
        actions = actions,
        playbackProgress = progress,
        queueEpisodeIds = queueEpisodeIds,
        subscribedFeedUrls = subscribedFeedUrls,
        twoPane = twoPane,
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
    )
}
