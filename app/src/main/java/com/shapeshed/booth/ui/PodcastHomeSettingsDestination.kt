package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Adapts home state and actions to the stateless application settings screen. */
@Composable
internal fun PodcastHomeSettingsDestination(
    homeUiState: PodcastHomeUiState,
    globalPlaybackSpeed: Float,
    viewModel: PodcastViewModel,
    playbackViewModel: PodcastPlaybackViewModel,
    platformActions: PodcastHomePlatformActions,
    onManagePodcasts: (PodcastManagementCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val podcastManagementCounts = PodcastManagementCounts(
        playbackSpeed = homeUiState.allPodcasts.count { it.playbackSpeed != null },
        autoRefresh = homeUiState.allPodcasts.count { it.includeInAutoRefresh },
        autoDownload = homeUiState.allPodcasts.count { it.includeInAutoDownload },
        autoQueue = homeUiState.allPodcasts.count { it.includeInAutoQueue },
        videoDownload = homeUiState.allPodcasts.count { it.includeInVideoDownload },
        notifications = homeUiState.allPodcasts.count { it.includeInNotifications },
    )
    val podcastIndexCredentials by viewModel.podcastIndexCredentials.collectAsStateWithLifecycle()

    PodcastAppSettingsContent(
        globalPlaybackSpeed = globalPlaybackSpeed,
        podcastManagementCounts = podcastManagementCounts,
        autoQueueEnabled = homeUiState.settings.autoQueueEnabled,
        onAutoQueueEnabledChange = viewModel::setPodcastAutoQueueEnabled,
        skipSilence = homeUiState.playback.skipSilence,
        onGlobalPlaybackSpeedChange = playbackViewModel::setSpeed,
        onSkipSilenceChange = playbackViewModel::setSkipSilence,
        videoDownloadsEnabled = homeUiState.settings.downloadVideos,
        onVideoDownloadsEnabledChange = viewModel::setPodcastDownloadVideos,
        autoRefreshEnabled = homeUiState.settings.autoRefreshEnabled,
        onAutoRefreshEnabledChange = viewModel::setPodcastAutoRefreshEnabled,
        refreshInterval = homeUiState.settings.refreshInterval,
        onRefreshIntervalChange = viewModel::setPodcastRefreshInterval,
        refreshNetwork = homeUiState.settings.refreshNetwork,
        onRefreshNetworkChange = viewModel::setPodcastRefreshNetwork,
        downloadNetwork = homeUiState.settings.downloadNetwork,
        onDownloadNetworkChange = viewModel::setPodcastDownloadNetwork,
        notificationsEnabled = homeUiState.settings.notificationsEnabled &&
            platformActions.notificationsPermissionGranted,
        onNotificationsEnabledChange = platformActions.setNotificationsEnabled,
        autoDownloadEnabled = homeUiState.settings.autoDownloadEnabled,
        onAutoDownloadEnabledChange = viewModel::setPodcastAutoDownloadEnabled,
        searchProviders = viewModel.searchProviders,
        selectedSearchProviderId = homeUiState.settings.searchProviderId,
        onSearchProviderChange = viewModel::setPodcastSearchProvider,
        podcastIndexCredentials = podcastIndexCredentials,
        onSavePodcastIndexCredentials = viewModel::setPodcastIndexCredentials,
        onClearPodcastIndexCredentials = viewModel::clearPodcastIndexCredentials,
        isImporting = homeUiState.homeState.isImporting,
        statusMessage = homeUiState.homeState.error,
        onImportOpml = platformActions.importOpml,
        onExportOpml = platformActions.exportOpml,
        onManagePodcasts = onManagePodcasts,
        modifier = modifier,
    )
}
