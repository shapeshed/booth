package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.booth.data.PodcastSubscriptionsViewMode

@Immutable
internal data class PodcastHomeUiState(
    val podcasts: List<com.shapeshed.booth.data.PodcastEntity>,
    val allPodcasts: List<com.shapeshed.booth.data.PodcastEntity>,
    val latestEpisodePublishedAt: Map<Long, Long?>,
    val podcastCatalogIndex: com.shapeshed.booth.data.PodcastCatalogIndex,
    val queueEntries: List<com.shapeshed.booth.data.QueueEntity>,
    val queueEpisodes: List<com.shapeshed.booth.data.EpisodeEntity>,
    val downloadAssets: List<com.shapeshed.booth.data.DownloadAssetEntity>,
    val downloadedEpisodes: Map<Long, com.shapeshed.booth.data.EpisodeEntity>,
    val previewEpisodeEntities: Map<Long, com.shapeshed.booth.data.EpisodeEntity>,
    val homeState: PodcastHomeState,
    val refreshing: Boolean,
    val refreshProgress: PodcastRefreshProgress,
    val subscriptionsViewMode: PodcastSubscriptionsViewMode,
    val playback: PlaybackUiState,
    val sleepTimer: com.shapeshed.booth.data.SleepTimerState?,
    val settings: com.shapeshed.booth.data.PodcastSettingsState,
    val downloadProgress: Map<Long, com.shapeshed.booth.data.DownloadProgress>,
)

@Composable
internal fun rememberPodcastHomeUiState(
    viewModel: PodcastViewModel,
    playbackViewModel: PodcastPlaybackViewModel,
): PodcastHomeUiState {
    val podcasts by viewModel.podcasts.collectAsStateWithLifecycle()
    val allPodcasts by viewModel.allPodcasts.collectAsStateWithLifecycle()
    val latestEpisodePublishedAt by viewModel.latestEpisodePublishedAt.collectAsStateWithLifecycle()
    val podcastCatalogIndex by viewModel.podcastCatalogIndex.collectAsStateWithLifecycle()
    val queueEntries by viewModel.queue.collectAsStateWithLifecycle()
    val queueEpisodes by viewModel.queueEpisodes.collectAsStateWithLifecycle()
    val downloadAssets by viewModel.downloadAssets.collectAsStateWithLifecycle()
    val downloadedEpisodes by viewModel.downloadedEpisodes.collectAsStateWithLifecycle()
    val previewEpisodeEntities by viewModel.previewEpisodeEntities.collectAsStateWithLifecycle()
    val homeState by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val refreshProgress by viewModel.refreshProgress.collectAsStateWithLifecycle()
    val subscriptionsViewMode by viewModel.subscriptionsViewMode.collectAsStateWithLifecycle()
    val playback by playbackViewModel.state.collectAsStateWithLifecycle()
    val sleepTimer by playbackViewModel.sleepTimer.collectAsStateWithLifecycle()
    val settings by viewModel.podcastSettings.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    return PodcastHomeUiState(
        podcasts = podcasts,
        allPodcasts = allPodcasts,
        latestEpisodePublishedAt = latestEpisodePublishedAt,
        podcastCatalogIndex = podcastCatalogIndex,
        queueEntries = queueEntries,
        queueEpisodes = queueEpisodes,
        downloadAssets = downloadAssets,
        downloadedEpisodes = downloadedEpisodes,
        previewEpisodeEntities = previewEpisodeEntities,
        homeState = homeState,
        refreshing = refreshing,
        refreshProgress = refreshProgress,
        subscriptionsViewMode = subscriptionsViewMode,
        playback = playback,
        sleepTimer = sleepTimer,
        settings = settings,
        downloadProgress = downloadProgress,
    )
}
