package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.booth.data.PodcastDownloadNetwork
import com.shapeshed.booth.data.PodcastIndexCredentials
import com.shapeshed.booth.data.PodcastRefreshInterval
import com.shapeshed.booth.data.PodcastRefreshNetwork
import com.shapeshed.booth.data.PodcastSearchProvider
import kotlinx.coroutines.flow.StateFlow

/**
 * The application settings screen's setters.
 *
 * This screen is almost entirely a list of toggles, so it was forwarding twelve setters straight
 * off the ViewModel. Twelve is well past the point where a parameter list stays readable, so they
 * are grouped here rather than spread across the destination's signature.
 *
 * Grouping them also means adding a setting does not add a parameter to every layer between the
 * ViewModel and the screen.
 */
internal class PodcastSettingsActions(
    val setAutoQueueEnabled: (Boolean) -> Unit,
    val setDownloadEpisodesAddedToUpNext: (Boolean) -> Unit,
    val setRefreshInterval: (PodcastRefreshInterval) -> Unit,
    val setRefreshNetwork: (PodcastRefreshNetwork) -> Unit,
    val setDownloadNetwork: (PodcastDownloadNetwork) -> Unit,
    val setRemovePlayedDownloads: (Boolean) -> Unit,
    val setSearchProvider: (String) -> Unit,
    val setPodcastIndexCredentials: (apiKey: String, apiSecret: String) -> Unit,
    val clearPodcastIndexCredentials: () -> Unit,
)

/** Builds the settings setters where the ViewModel is in scope. */
@Composable
internal fun rememberSettingsActions(viewModel: PodcastViewModel): PodcastSettingsActions = remember(viewModel) {
    PodcastSettingsActions(
        setAutoQueueEnabled = viewModel::setPodcastAutoQueueEnabled,
        setDownloadEpisodesAddedToUpNext = viewModel::setPodcastDownloadEpisodesAddedToUpNext,
        setRefreshInterval = viewModel::setPodcastRefreshInterval,
        setRefreshNetwork = viewModel::setPodcastRefreshNetwork,
        setDownloadNetwork = viewModel::setPodcastDownloadNetwork,
        setRemovePlayedDownloads = viewModel::setPodcastRemovePlayedDownloads,
        setSearchProvider = viewModel::setPodcastSearchProvider,
        setPodcastIndexCredentials = { apiKey, apiSecret ->
            viewModel.setPodcastIndexCredentials(apiKey, apiSecret)
        },
        clearPodcastIndexCredentials = viewModel::clearPodcastIndexCredentials,
    )
}

/** Adapts home state and actions to the stateless application settings screen. */
@Composable
internal fun PodcastHomeSettingsDestination(
    homeUiState: PodcastHomeUiState,
    actions: PodcastSettingsActions,
    searchProviders: List<PodcastSearchProvider>,
    podcastIndexCredentials: StateFlow<PodcastIndexCredentials?>,
    platformActions: PodcastHomePlatformActions,
    onManagePodcasts: (PodcastManagementCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val podcastManagementCounts = PodcastManagementCounts(
        total = homeUiState.allPodcasts.size,
        autoRefresh = homeUiState.allPodcasts.count { it.includeInAutoRefresh },
        autoQueue = homeUiState.allPodcasts.count { it.includeInAutoQueue },
        notifications = homeUiState.allPodcasts.count { it.includeInNotifications },
    )
    // Collected here rather than in a parent: this is the only screen that edits credentials, and
    // it reads once on entry.
    val credentials by podcastIndexCredentials.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Calls PodcastAppSettingsScreen directly. There used to be a PodcastAppSettingsContent
    // adapter in between that forwarded all 34 parameters verbatim and added nothing.
    PodcastAppSettingsScreen(
        podcastManagementCounts = podcastManagementCounts,
        autoQueueEnabled = homeUiState.settings.autoQueueEnabled,
        onAutoQueueEnabledChange = actions.setAutoQueueEnabled,
        downloadEpisodesAddedToUpNext = homeUiState.settings.downloadEpisodesAddedToUpNext,
        onDownloadEpisodesAddedToUpNextChange = actions.setDownloadEpisodesAddedToUpNext,
        refreshInterval = homeUiState.settings.refreshInterval,
        onRefreshIntervalChange = actions.setRefreshInterval,
        refreshNetwork = homeUiState.settings.refreshNetwork,
        onRefreshNetworkChange = actions.setRefreshNetwork,
        downloadNetwork = homeUiState.settings.downloadNetwork,
        onDownloadNetworkChange = actions.setDownloadNetwork,
        removePlayedDownloads = homeUiState.settings.removePlayedDownloads,
        onRemovePlayedDownloadsChange = actions.setRemovePlayedDownloads,
        notificationsEnabled = homeUiState.settings.notificationsEnabled &&
            platformActions.notificationsPermissionGranted,
        onNotificationsEnabledChange = platformActions.setNotificationsEnabled,
        searchProviders = searchProviders,
        selectedSearchProviderId = homeUiState.settings.searchProviderId,
        onSearchProviderChange = actions.setSearchProvider,
        podcastIndexCredentials = credentials,
        onSavePodcastIndexCredentials = actions.setPodcastIndexCredentials,
        onClearPodcastIndexCredentials = actions.clearPodcastIndexCredentials,
        isImporting = homeUiState.homeState.isImporting,
        statusMessage = homeUiState.homeState.error,
        onImportOpml = platformActions.importOpml,
        onExportOpml = platformActions.exportOpml,
        onExportBackup = platformActions.exportBackup,
        onExportBackupZip = platformActions.exportBackupZip,
        onImportBackup = platformActions.importBackup,
        onManagePodcasts = onManagePodcasts,
        onCopyVersion = { label -> copyTextToClipboard(context, label, clipLabel = "version") },
        modifier = modifier,
    )
}
