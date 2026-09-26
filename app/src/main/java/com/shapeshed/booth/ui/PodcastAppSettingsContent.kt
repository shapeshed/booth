package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.PodcastRefreshInterval
import com.shapeshed.booth.data.PodcastRefreshNetwork
import com.shapeshed.booth.data.PodcastDownloadNetwork
import com.shapeshed.booth.data.PodcastDownloadLimit
import com.shapeshed.booth.data.PodcastDeleteBeforeAutoDownload
import com.shapeshed.booth.data.PodcastSearchProvider
import com.shapeshed.booth.data.PodcastIndexCredentials

internal data class PodcastManagementCounts(
    val total: Int,
    val autoRefresh: Int,
    val autoQueue: Int,
    val notifications: Int,
)

@Composable
internal fun PodcastAppSettingsContent(
    podcastManagementCounts: PodcastManagementCounts,
    autoQueueEnabled: Boolean,
    onAutoQueueEnabledChange: (Boolean) -> Unit,
    downloadEpisodesAddedToUpNext: Boolean,
    onDownloadEpisodesAddedToUpNextChange: (Boolean) -> Unit,
    refreshInterval: PodcastRefreshInterval,
    onRefreshIntervalChange: (PodcastRefreshInterval) -> Unit,
    refreshNetwork: PodcastRefreshNetwork,
    onRefreshNetworkChange: (PodcastRefreshNetwork) -> Unit,
    downloadNetwork: PodcastDownloadNetwork,
    onDownloadNetworkChange: (PodcastDownloadNetwork) -> Unit,
    downloadLimit: PodcastDownloadLimit,
    onDownloadLimitChange: (PodcastDownloadLimit) -> Unit,
    deleteBeforeAutoDownload: PodcastDeleteBeforeAutoDownload,
    onDeleteBeforeAutoDownloadChange: (PodcastDeleteBeforeAutoDownload) -> Unit,
    removePlayedDownloads: Boolean,
    onRemovePlayedDownloadsChange: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    searchProviders: List<PodcastSearchProvider>,
    selectedSearchProviderId: String,
    onSearchProviderChange: (String) -> Unit,
    podcastIndexCredentials: PodcastIndexCredentials?,
    onSavePodcastIndexCredentials: (String, String) -> Unit,
    onClearPodcastIndexCredentials: () -> Unit,
    isImporting: Boolean,
    statusMessage: PodcastUiError?,
    onImportOpml: () -> Unit,
    onExportOpml: () -> Unit,
    onExportBackup: () -> Unit = {},
    onExportBackupZip: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onManagePodcasts: (PodcastManagementCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    PodcastAppSettingsScreen(
        podcastManagementCounts = podcastManagementCounts,
        autoQueueEnabled = autoQueueEnabled,
        onAutoQueueEnabledChange = onAutoQueueEnabledChange,
        downloadEpisodesAddedToUpNext = downloadEpisodesAddedToUpNext,
        onDownloadEpisodesAddedToUpNextChange = onDownloadEpisodesAddedToUpNextChange,
        refreshInterval = refreshInterval,
        onRefreshIntervalChange = onRefreshIntervalChange,
        refreshNetwork = refreshNetwork,
        onRefreshNetworkChange = onRefreshNetworkChange,
        downloadNetwork = downloadNetwork,
        onDownloadNetworkChange = onDownloadNetworkChange,
        downloadLimit = downloadLimit,
        onDownloadLimitChange = onDownloadLimitChange,
        deleteBeforeAutoDownload = deleteBeforeAutoDownload,
        onDeleteBeforeAutoDownloadChange = onDeleteBeforeAutoDownloadChange,
        removePlayedDownloads = removePlayedDownloads,
        onRemovePlayedDownloadsChange = onRemovePlayedDownloadsChange,
        notificationsEnabled = notificationsEnabled,
        onNotificationsEnabledChange = onNotificationsEnabledChange,
        searchProviders = searchProviders,
        selectedSearchProviderId = selectedSearchProviderId,
        onSearchProviderChange = onSearchProviderChange,
        podcastIndexCredentials = podcastIndexCredentials,
        onSavePodcastIndexCredentials = onSavePodcastIndexCredentials,
        onClearPodcastIndexCredentials = onClearPodcastIndexCredentials,
        isImporting = isImporting,
        statusMessage = statusMessage,
        onImportOpml = onImportOpml,
        onExportOpml = onExportOpml,
        onExportBackup = onExportBackup,
        onExportBackupZip = onExportBackupZip,
        onImportBackup = onImportBackup,
        onManagePodcasts = onManagePodcasts,
        modifier = modifier,
    )
}
