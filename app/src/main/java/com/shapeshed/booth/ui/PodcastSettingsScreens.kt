package com.shapeshed.booth.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.shapeshed.booth.BuildConfig
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.PodcastRefreshInterval
import com.shapeshed.booth.data.PodcastRefreshNetwork
import com.shapeshed.booth.data.PodcastDownloadNetwork
import com.shapeshed.booth.data.PodcastDownloadLimit
import com.shapeshed.booth.data.PodcastDeleteBeforeAutoDownload
import com.shapeshed.booth.data.PodcastSearchProvider
import com.shapeshed.booth.data.PodcastIndexCredentials


@Composable
internal fun PodcastAppSettingsScreen(
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
    downloadLimit: PodcastDownloadLimit = PodcastDownloadLimit.FIFTY,
    onDownloadLimitChange: (PodcastDownloadLimit) -> Unit = {},
    deleteBeforeAutoDownload: PodcastDeleteBeforeAutoDownload = PodcastDeleteBeforeAutoDownload.PLAYED,
    onDeleteBeforeAutoDownloadChange: (PodcastDeleteBeforeAutoDownload) -> Unit = {},
    removePlayedDownloads: Boolean = true,
    onRemovePlayedDownloadsChange: (Boolean) -> Unit = {},
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
    var showDownloadNetworkChooser by rememberSaveable { mutableStateOf(false) }
    var showDownloadLimitChooser by rememberSaveable { mutableStateOf(false) }
    var showDeleteBeforeAutoDownloadChooser by rememberSaveable { mutableStateOf(false) }
    var showRefreshIntervalChooser by rememberSaveable { mutableStateOf(false) }
    var showRefreshNetworkChooser by rememberSaveable { mutableStateOf(false) }
    var showSearchProviderChooser by rememberSaveable { mutableStateOf(false) }
    var showPodcastIndexCredentials by rememberSaveable { mutableStateOf(false) }
    val downloadNetworkLabel = when (downloadNetwork) {
        PodcastDownloadNetwork.ANY_CONNECTION -> stringResource(R.string.download_wifi_or_mobile)
        PodcastDownloadNetwork.WIFI_ONLY -> stringResource(R.string.download_wifi_only)
    }
    val refreshIntervalLabel = when (refreshInterval) {
        PodcastRefreshInterval.HOURLY -> stringResource(R.string.refresh_every_hour)
        PodcastRefreshInterval.SIX_HOURS -> stringResource(R.string.refresh_every_six_hours)
        PodcastRefreshInterval.DAILY -> stringResource(R.string.refresh_daily)
    }
    val refreshNetworkLabel = when (refreshNetwork) {
        PodcastRefreshNetwork.ANY_CONNECTION -> stringResource(R.string.download_wifi_or_mobile)
        PodcastRefreshNetwork.WIFI_ONLY -> stringResource(R.string.download_wifi_only)
    }
    val downloadLimitLabel = when (downloadLimit) {
        PodcastDownloadLimit.TWENTY_FIVE -> stringResource(R.string.download_limit_25)
        PodcastDownloadLimit.FIFTY -> stringResource(R.string.download_limit_50)
        PodcastDownloadLimit.ONE_HUNDRED -> stringResource(R.string.download_limit_100)
        PodcastDownloadLimit.UNLIMITED -> stringResource(R.string.download_limit_unlimited)
    }
    val deleteBeforeAutoDownloadLabel = when (deleteBeforeAutoDownload) {
        PodcastDeleteBeforeAutoDownload.OFF -> stringResource(R.string.download_cleanup_off)
        PodcastDeleteBeforeAutoDownload.PLAYED -> stringResource(R.string.download_cleanup_played)
        PodcastDeleteBeforeAutoDownload.ALL_ELIGIBLE -> stringResource(R.string.download_cleanup_all_eligible)
    }
    val autoQueueCountLabel = if (
        podcastManagementCounts.total > 0 &&
        podcastManagementCounts.autoQueue == podcastManagementCounts.total
    ) {
        stringResource(R.string.all_podcasts)
    } else {
        pluralStringResource(
            R.plurals.podcast_count,
            podcastManagementCounts.autoQueue,
            podcastManagementCounts.autoQueue,
        )
    }
    val notificationCountLabel = if (
        podcastManagementCounts.total > 0 &&
        podcastManagementCounts.notifications == podcastManagementCounts.total
    ) {
        stringResource(R.string.all_podcasts)
    } else {
        pluralStringResource(
            R.plurals.podcast_count,
            podcastManagementCounts.notifications,
            podcastManagementCounts.notifications,
        )
    }
    val versionLabel = BuildConfig.BUILD_LABEL.takeIf { it.isNotBlank() }?.let { label ->
        stringResource(R.string.build_label_format, label)
    } ?: stringResource(R.string.version_format, BuildConfig.VERSION_NAME)
    val context = LocalContext.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .testTag("podcast_app_settings_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 12.dp + LocalPodcastMiniPlayerInset.current,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            SettingsGroupLabel(text = stringResource(R.string.up_next))
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.automatically_add_to_up_next)) },
                supportingContent = { Text(stringResource(if (autoQueueEnabled) R.string.on else R.string.off)) },
                leadingContent = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = autoQueueEnabled,
                        onCheckedChange = onAutoQueueEnabledChange,
                    )
                },
                modifier = Modifier.clickable { onAutoQueueEnabledChange(!autoQueueEnabled) },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.podcast_auto_queue)) },
                supportingContent = {
                    Text(if (autoQueueEnabled) autoQueueCountLabel else stringResource(R.string.disabled_globally))
                },
                leadingContent = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                modifier = Modifier.clickable(enabled = autoQueueEnabled) {
                    onManagePodcasts(PodcastManagementCategory.AUTO_QUEUE)
                },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.download_up_next)) },
                supportingContent = { Text(stringResource(if (downloadEpisodesAddedToUpNext) R.string.on else R.string.off)) },
                leadingContent = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = downloadEpisodesAddedToUpNext,
                        onCheckedChange = onDownloadEpisodesAddedToUpNextChange,
                    )
                },
                modifier = Modifier.clickable {
                    onDownloadEpisodesAddedToUpNextChange(!downloadEpisodesAddedToUpNext)
                },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.download_network)) },
                supportingContent = { Text(downloadNetworkLabel) },
                leadingContent = { Icon(Icons.Rounded.Wifi, contentDescription = null) },
                modifier = Modifier.clickable { showDownloadNetworkChooser = true },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.download_limit)) },
                supportingContent = { Text(downloadLimitLabel) },
                leadingContent = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable { showDownloadLimitChooser = true },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.delete_before_auto_download)) },
                supportingContent = { Text(deleteBeforeAutoDownloadLabel) },
                leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                modifier = Modifier.clickable { showDeleteBeforeAutoDownloadChooser = true },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.remove_played_downloads)) },
                supportingContent = {
                    Text(stringResource(if (removePlayedDownloads) R.string.remove_played_downloads_summary else R.string.off))
                },
                leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = removePlayedDownloads,
                        onCheckedChange = onRemovePlayedDownloadsChange,
                    )
                },
                modifier = Modifier.clickable {
                    onRemovePlayedDownloadsChange(!removePlayedDownloads)
                },
            )
        }
        item {
            SettingsGroupLabel(text = stringResource(R.string.auto_refresh))
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.refresh_podcasts_automatically)) },
                supportingContent = {
                    val autoRefreshCountLabel = if (
                        podcastManagementCounts.total > 0 &&
                        podcastManagementCounts.autoRefresh == podcastManagementCounts.total
                    ) {
                        stringResource(R.string.all_podcasts)
                    } else {
                        pluralStringResource(
                            R.plurals.podcast_count,
                            podcastManagementCounts.autoRefresh,
                            podcastManagementCounts.autoRefresh,
                        )
                    }
                    Text(autoRefreshCountLabel)
                },
                leadingContent = { Icon(Icons.Rounded.Sync, contentDescription = null) },
                modifier = Modifier.clickable { onManagePodcasts(PodcastManagementCategory.AUTO_REFRESH) },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.refresh_interval)) },
                supportingContent = { Text(refreshIntervalLabel) },
                leadingContent = { Icon(Icons.Rounded.Sync, contentDescription = null) },
                modifier = Modifier.clickable { showRefreshIntervalChooser = true },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.refresh_network)) },
                supportingContent = { Text(refreshNetworkLabel) },
                leadingContent = { Icon(Icons.Rounded.Wifi, contentDescription = null) },
                modifier = Modifier.clickable { showRefreshNetworkChooser = true },
            )
        }
        item {
            SettingsGroupLabel(text = stringResource(R.string.notifications))
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.notifications)) },
                supportingContent = { Text(stringResource(if (notificationsEnabled) R.string.on else R.string.off)) },
                leadingContent = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationsEnabledChange,
                    )
                },
                modifier = Modifier.clickable { onNotificationsEnabledChange(!notificationsEnabled) },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.podcast_notifications)) },
                supportingContent = {
                    Text(if (notificationsEnabled) notificationCountLabel else stringResource(R.string.disabled_globally))
                },
                leadingContent = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                modifier = Modifier.clickable(enabled = notificationsEnabled) {
                    onManagePodcasts(PodcastManagementCategory.NOTIFICATIONS)
                },
            )
        }
        item {
            SettingsGroupLabel(text = stringResource(R.string.discovery_settings_group))
        }
        if (searchProviders.isNotEmpty()) {
            item {
                PodcastActionListItem(
                    headlineContent = { Text(stringResource(R.string.podcast_search)) },
                    supportingContent = {
                        Text(searchProviders.firstOrNull { it.id == selectedSearchProviderId }?.displayName ?: stringResource(R.string.default_value))
                    },
                    leadingContent = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier.clickable { showSearchProviderChooser = true },
                )
            }
        }
        if (selectedSearchProviderId == "podcast-index") {
            item {
                PodcastActionListItem(
                    headlineContent = { Text(stringResource(R.string.podcast_index_access)) },
                    supportingContent = {
                        Text(stringResource(if (podcastIndexCredentials == null) R.string.optional_read_only_credentials else R.string.credentials_configured))
                    },
                    leadingContent = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    modifier = Modifier.clickable { showPodcastIndexCredentials = true },
                )
            }
        }
        item { SettingsGroupLabel(text = stringResource(R.string.import_and_export)) }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.import_opml)) },
                supportingContent = { Text(stringResource(R.string.import_opml_podcasts_summary)) },
                leadingContent = { Icon(Icons.Rounded.UploadFile, contentDescription = null) },
                modifier = Modifier.clickable(enabled = !isImporting, onClick = onImportOpml),
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.export_opml)) },
                supportingContent = { Text(stringResource(R.string.export_opml_podcasts_summary)) },
                leadingContent = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onExportOpml),
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.import_backup)) },
                supportingContent = { Text(stringResource(R.string.import_backup_summary)) },
                leadingContent = { Icon(Icons.Rounded.UploadFile, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onImportBackup),
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.export_backup)) },
                supportingContent = { Text(stringResource(R.string.export_backup_summary)) },
                leadingContent = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onExportBackup),
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.export_backup_zip)) },
                supportingContent = { Text(stringResource(R.string.export_backup_zip_summary)) },
                leadingContent = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onExportBackupZip),
            )
        }
        if (statusMessage != null) {
            item {
                Text(
                    text = podcastErrorMessage(statusMessage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
        item {
            Text(
                text = versionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-version")
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("version", versionLabel))
                    }
                    .padding(vertical = 16.dp),
            )
        }
    }
    }
    if (showDownloadNetworkChooser) {
        AlertDialog(
            onDismissRequest = { showDownloadNetworkChooser = false },
            title = { Text(stringResource(R.string.download_network)) },
            text = {
                Column {
                    PodcastDownloadNetwork.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDownloadNetworkChange(option)
                                    showDownloadNetworkChooser = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = downloadNetwork == option,
                                onClick = {
                                    onDownloadNetworkChange(option)
                                    showDownloadNetworkChooser = false
                                },
                            )
                            Text(
                                when (option) {
                                    PodcastDownloadNetwork.ANY_CONNECTION -> stringResource(R.string.download_wifi_or_mobile)
                                    PodcastDownloadNetwork.WIFI_ONLY -> stringResource(R.string.download_wifi_only)
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDownloadNetworkChooser = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showDownloadLimitChooser) {
        AlertDialog(
            onDismissRequest = { showDownloadLimitChooser = false },
            title = { Text(stringResource(R.string.download_limit)) },
            text = {
                Column {
                    PodcastDownloadLimit.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDownloadLimitChange(option)
                                    showDownloadLimitChooser = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = downloadLimit == option,
                                onClick = {
                                    onDownloadLimitChange(option)
                                    showDownloadLimitChooser = false
                                },
                            )
                            Text(
                                text = when (option) {
                                    PodcastDownloadLimit.TWENTY_FIVE -> stringResource(R.string.download_limit_25)
                                    PodcastDownloadLimit.FIFTY -> stringResource(R.string.download_limit_50)
                                    PodcastDownloadLimit.ONE_HUNDRED -> stringResource(R.string.download_limit_100)
                                    PodcastDownloadLimit.UNLIMITED -> stringResource(R.string.download_limit_unlimited)
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDownloadLimitChooser = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showDeleteBeforeAutoDownloadChooser) {
        AlertDialog(
            onDismissRequest = { showDeleteBeforeAutoDownloadChooser = false },
            title = { Text(stringResource(R.string.delete_before_auto_download)) },
            text = {
                Column {
                    PodcastDeleteBeforeAutoDownload.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDeleteBeforeAutoDownloadChange(option)
                                    showDeleteBeforeAutoDownloadChooser = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = deleteBeforeAutoDownload == option,
                                onClick = {
                                    onDeleteBeforeAutoDownloadChange(option)
                                    showDeleteBeforeAutoDownloadChooser = false
                                },
                            )
                            Text(
                                text = when (option) {
                                    PodcastDeleteBeforeAutoDownload.OFF -> stringResource(R.string.download_cleanup_off)
                                    PodcastDeleteBeforeAutoDownload.PLAYED -> stringResource(R.string.download_cleanup_played)
                                    PodcastDeleteBeforeAutoDownload.ALL_ELIGIBLE -> stringResource(R.string.download_cleanup_all_eligible)
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDeleteBeforeAutoDownloadChooser = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showRefreshIntervalChooser) {
        AlertDialog(
            onDismissRequest = { showRefreshIntervalChooser = false },
            title = { Text(stringResource(R.string.refresh_interval)) },
            text = {
                Column {
                    PodcastRefreshInterval.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRefreshIntervalChange(option)
                                    showRefreshIntervalChooser = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = refreshInterval == option,
                                onClick = {
                                    onRefreshIntervalChange(option)
                                    showRefreshIntervalChooser = false
                                },
                            )
                            Text(
                                when (option) {
                                    PodcastRefreshInterval.HOURLY -> stringResource(R.string.refresh_every_hour)
                                    PodcastRefreshInterval.SIX_HOURS -> stringResource(R.string.refresh_every_six_hours)
                                    PodcastRefreshInterval.DAILY -> stringResource(R.string.refresh_daily)
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRefreshIntervalChooser = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showRefreshNetworkChooser) {
        AlertDialog(
            onDismissRequest = { showRefreshNetworkChooser = false },
            title = { Text(stringResource(R.string.refresh_network)) },
            text = {
                Column {
                    PodcastRefreshNetwork.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRefreshNetworkChange(option)
                                    showRefreshNetworkChooser = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = refreshNetwork == option,
                                onClick = {
                                    onRefreshNetworkChange(option)
                                    showRefreshNetworkChooser = false
                                },
                            )
                            Text(
                                when (option) {
                                    PodcastRefreshNetwork.ANY_CONNECTION -> stringResource(R.string.download_wifi_or_mobile)
                                    PodcastRefreshNetwork.WIFI_ONLY -> stringResource(R.string.download_wifi_only)
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRefreshNetworkChooser = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showSearchProviderChooser) {
        AlertDialog(
            onDismissRequest = { showSearchProviderChooser = false },
            title = { Text(stringResource(R.string.podcast_search_provider)) },
            text = {
                Column {
                    searchProviders.forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSearchProviderChange(provider.id)
                                    showSearchProviderChooser = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedSearchProviderId == provider.id,
                                onClick = {
                                    onSearchProviderChange(provider.id)
                                    showSearchProviderChooser = false
                                },
                            )
                            Text(provider.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchProviderChooser = false }) { Text(stringResource(R.string.done)) }
            },
        )
    }
    if (showPodcastIndexCredentials) {
        PodcastIndexCredentialsDialog(
            hasSavedCredentials = podcastIndexCredentials != null,
            onDismiss = { showPodcastIndexCredentials = false },
            onSave = { key, secret ->
                onSavePodcastIndexCredentials(key, secret)
                showPodcastIndexCredentials = false
            },
            onClear = {
                onClearPodcastIndexCredentials()
                showPodcastIndexCredentials = false
            },
        )
    }
}

@Composable
private fun PodcastIndexCredentialsDialog(
    hasSavedCredentials: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var apiSecret by rememberSaveable { mutableStateOf("") }
    val hasPartialInput = apiKey.isNotBlank() xor apiSecret.isNotBlank()
    val canSave = !hasPartialInput && (hasSavedCredentials || (apiKey.isNotBlank() && apiSecret.isNotBlank()))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.podcast_index_access)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (hasSavedCredentials) {
                        stringResource(R.string.credentials_saved_summary)
                    } else {
                        stringResource(R.string.credentials_optional_summary)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.api_key)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = apiSecret,
                    onValueChange = { apiSecret = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.api_secret)) },
                    singleLine = true,
                    isError = hasPartialInput,
                    supportingText = if (hasPartialInput) {
                        { Text(stringResource(R.string.enter_both_credentials)) }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        dismissButton = {
            if (hasSavedCredentials) TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
        },
        confirmButton = {
            TextButton(onClick = { if (apiKey.isNotBlank()) onSave(apiKey, apiSecret) else onDismiss() }, enabled = canSave) {
                Text(stringResource(if (hasSavedCredentials && apiKey.isBlank()) R.string.done else R.string.save))
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PodcastSettingsScreen(
    podcast: PodcastEntity,
    globalPlaybackSpeed: Float,
    globalSkipSilence: Boolean,
    onPlaybackSpeedChange: (Float?) -> Unit,
    onSkipSilenceChange: (Boolean?) -> Unit,
    globalAutoQueueEnabled: Boolean,
    globalNotificationsEnabled: Boolean,
    availableTags: List<String>,
    onSaveSettings: (String, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tags by rememberSaveable(podcast.id) {
        mutableStateOf(
            podcast.categories.filter { it.providerId == com.shapeshed.booth.data.LOCAL_DIRECTORY_PROVIDER_ID }
                .map { it.name },
        )
    }
    var tagInput by rememberSaveable(podcast.id) { mutableStateOf("") }
    var skipStartInput by rememberSaveable(podcast.id) { mutableStateOf(podcast.skipStartSeconds.toString()) }
    var skipEndInput by rememberSaveable(podcast.id) { mutableStateOf(podcast.skipEndSeconds.toString()) }
    var includeInAutoRefresh by rememberSaveable(podcast.id) { mutableStateOf(podcast.includeInAutoRefresh) }
    var includeInAutoQueue by rememberSaveable(podcast.id) { mutableStateOf(podcast.includeInAutoQueue) }
    var includeInNotifications by rememberSaveable(podcast.id) { mutableStateOf(podcast.includeInNotifications) }
    var showTagsEditor by rememberSaveable(podcast.id) { mutableStateOf(false) }
    var showSkipEditor by rememberSaveable(podcast.id) { mutableStateOf(false) }
    var tagMenuExpanded by rememberSaveable(podcast.id) { mutableStateOf(false) }
    var showPlaybackSpeedEditor by rememberSaveable(podcast.id) { mutableStateOf(false) }

    fun tagsWithPendingInput(): List<String> {
        val tag = tagInput.trim()
        return if (tag.isNotEmpty() && tags.none { it.equals(tag, ignoreCase = true) }) {
            tags + tag
        } else {
            tags
        }
    }

    fun addTag() {
        tags = tagsWithPendingInput()
        tagInput = ""
    }

    val skipSummary = buildList {
        skipStartInput.toIntOrNull()?.takeIf { it > 0 }?.let { add(stringResource(R.string.skip_start_summary, it)) }
        skipEndInput.toIntOrNull()?.takeIf { it > 0 }?.let { add(stringResource(R.string.skip_end_summary, it)) }
    }.ifEmpty { listOf(stringResource(R.string.skip_off)) }.joinToString(" · ")
    val tagSuggestions = availableTags
        .filter { suggestion -> tags.none { it.equals(suggestion, ignoreCase = true) } }
        .filter { suggestion -> tagInput.isBlank() || suggestion.contains(tagInput.trim(), ignoreCase = true) }
        .take(8)

    fun persistSettings(
        nextTags: List<String> = tags,
        nextSkipStart: String = skipStartInput,
        nextSkipEnd: String = skipEndInput,
        nextAutoRefresh: Boolean = includeInAutoRefresh,
        nextAutoQueue: Boolean = includeInAutoQueue,
        nextNotifications: Boolean = includeInNotifications,
    ) {
        onSaveSettings(
            nextTags.joinToString(","),
            nextSkipStart.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            nextSkipEnd.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            nextAutoRefresh,
            podcast.includeInAutoDownload,
            nextAutoQueue,
            nextNotifications,
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 24.dp + LocalPodcastMiniPlayerInset.current,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsGroupLabel(text = stringResource(R.string.playback))
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.playback_speed)) },
                supportingContent = {
                    Text(
                        podcast.playbackSpeed?.let { formatPlaybackSpeed(it) }
                            ?: stringResource(R.string.inherited_global_playback_speed, formatPlaybackSpeed(globalPlaybackSpeed)),
                    )
                },
                leadingContent = { Icon(Icons.Outlined.Speed, contentDescription = null) },
                modifier = Modifier.clickable { showPlaybackSpeedEditor = true },
            )
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.skip_intro_outro)) },
                supportingContent = { Text(skipSummary) },
                leadingContent = { Icon(Icons.Rounded.FastForward, contentDescription = null) },
                modifier = Modifier.clickable { showSkipEditor = true },
            )
        }
        item {
            SettingsGroupLabel(text = stringResource(R.string.auto_refresh))
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.refresh_podcasts_automatically)) },
                supportingContent = { Text(stringResource(if (includeInAutoRefresh) R.string.on else R.string.off)) },
                leadingContent = { Icon(Icons.Rounded.Sync, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = includeInAutoRefresh,
                        onCheckedChange = {
                            includeInAutoRefresh = it
                            persistSettings(nextAutoRefresh = it)
                        },
                    )
                },
                modifier = Modifier.clickable {
                    val next = !includeInAutoRefresh
                    includeInAutoRefresh = next
                    persistSettings(nextAutoRefresh = next)
                },
            )
        }
        item {
            SettingsGroupLabel(text = stringResource(R.string.up_next))
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.add_new_episodes_to_up_next)) },
                supportingContent = {
                    Text(
                        stringResource(
                            when {
                                !globalAutoQueueEnabled -> R.string.disabled_globally
                                includeInAutoQueue -> R.string.on
                                else -> R.string.off
                            },
                        ),
                    )
                },
                leadingContent = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = includeInAutoQueue,
                        onCheckedChange = {
                            includeInAutoQueue = it
                            persistSettings(nextAutoQueue = it)
                        },
                        enabled = globalAutoQueueEnabled,
                    )
                },
                modifier = Modifier.clickable(enabled = globalAutoQueueEnabled) {
                    val next = !includeInAutoQueue
                    includeInAutoQueue = next
                    persistSettings(nextAutoQueue = next)
                },
            )
        }
        item {
            SettingsGroupLabel(text = stringResource(R.string.notifications))
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.notifications)) },
                supportingContent = {
                    Text(
                        stringResource(
                            when {
                                !globalNotificationsEnabled -> R.string.disabled_globally
                                includeInNotifications -> R.string.on
                                else -> R.string.off
                            },
                        ),
                    )
                },
                leadingContent = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = includeInNotifications,
                        onCheckedChange = {
                            includeInNotifications = it
                            persistSettings(nextNotifications = it)
                        },
                        enabled = globalNotificationsEnabled,
                    )
                },
                modifier = Modifier.clickable(enabled = globalNotificationsEnabled) {
                    val next = !includeInNotifications
                    includeInNotifications = next
                    persistSettings(nextNotifications = next)
                },
            )
        }
        item {
            SettingsGroupLabel(text = stringResource(R.string.organization))
        }
        item {
            PodcastActionListItem(
                headlineContent = { Text(stringResource(R.string.tags)) },
                supportingContent = {
                    Text(tags.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: stringResource(R.string.no_tags))
                },
                leadingContent = { Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = null) },
                modifier = Modifier.clickable { showTagsEditor = true },
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
        }
    }
    }
    if (showTagsEditor) {
        AlertDialog(
            onDismissRequest = {
                tagMenuExpanded = false
                showTagsEditor = false
            },
            title = { Text(stringResource(R.string.tags)) },
            text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags, key = { it }) { tag ->
                            InputChip(
                                selected = false,
                                onClick = { tags = tags - tag },
                                label = { Text(tag) },
                                trailingIcon = { Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.remove_tag, tag)) },
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = tagMenuExpanded && tagSuggestions.isNotEmpty(),
                    onExpandedChange = { tagMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = {
                            tagInput = it
                            tagMenuExpanded = true
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                            .fillMaxWidth()
                            .onFocusChanged { if (it.isFocused) tagMenuExpanded = true },
                        singleLine = true,
                        label = { Text(stringResource(R.string.add_tag)) },
                        trailingIcon = {
                            if (tagInput.isNotBlank()) {
                                IconButton(onClick = ::addTag) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_tag)) }
                            }
                        },
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { addTag() }),
                    )
                    ExposedDropdownMenu(
                        expanded = tagMenuExpanded && tagSuggestions.isNotEmpty(),
                        onDismissRequest = { tagMenuExpanded = false },
                    ) {
                        tagSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = null) },
                                onClick = {
                                    tags = tags + suggestion
                                    tagInput = ""
                                    tagMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            },
            confirmButton = {
                TextButton(onClick = {
                    persistSettings(nextTags = tagsWithPendingInput())
                    tagMenuExpanded = false
                    showTagsEditor = false
                }) { Text(stringResource(R.string.done)) }
            },
        )
    }
    if (showSkipEditor) {
        AlertDialog(
            onDismissRequest = { showSkipEditor = false },
            title = { Text(stringResource(R.string.skip_intro_outro)) },
            text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.skip_intro_outro_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = skipStartInput,
                    onValueChange = { value -> if (value.all(Char::isDigit)) skipStartInput = value },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.skip_at_start_seconds)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                )
                OutlinedTextField(
                    value = skipEndInput,
                    onValueChange = { value -> if (value.all(Char::isDigit)) skipEndInput = value },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.skip_at_end_seconds)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                )
            }
            },
            confirmButton = {
                TextButton(onClick = {
                    persistSettings()
                    showSkipEditor = false
                }) { Text(stringResource(R.string.done)) }
            },
        )
    }
    if (showPlaybackSpeedEditor) {
        PlaybackSpeedSheet(
            speed = podcast.playbackSpeed ?: globalPlaybackSpeed,
            skipSilence = podcast.skipSilence ?: globalSkipSilence,
            onSpeedChange = { onPlaybackSpeedChange(it); showPlaybackSpeedEditor = false },
            onSkipSilenceChange = { enabled -> onSkipSilenceChange(enabled) },
            onDismiss = { showPlaybackSpeedEditor = false },
            inherited = podcast.playbackSpeed == null,
            globalSpeed = globalPlaybackSpeed,
            onUseGlobal = { onPlaybackSpeedChange(null); showPlaybackSpeedEditor = false },
            inheritedSkipSilence = podcast.skipSilence == null,
            globalSkipSilence = globalSkipSilence,
            onUseGlobalSkipSilence = { onSkipSilenceChange(null) },
        )
    }
}
