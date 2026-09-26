package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
internal fun PodcastHomeRootActions(
    selectedTab: PodcastTab,
    queueReorderMode: Boolean,
    queueHasItems: Boolean,
    menuExpanded: Boolean,
    showSearchAction: Boolean = true,
    onClearInbox: () -> Unit,
    onClearQueue: () -> Unit,
    onQueueReorderDone: () -> Unit,
    onOpenDiscoverySearch: () -> Unit,
    onOpenAddPodcast: () -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAllEpisodes: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val showClearQueueConfirmation = rememberSaveable { mutableStateOf(false) }
    if (selectedTab == PodcastTab.UP_NEXT && queueReorderMode) {
        TextButton(onClick = onQueueReorderDone) {
            Text(stringResource(R.string.done))
        }
    } else {
        if (showSearchAction) {
            IconButton(onClick = onOpenDiscoverySearch) {
                Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
            }
        }
        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
            ) {
                if (selectedTab == PodcastTab.HOME) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove_all_from_inbox)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        onClick = {
                            onMenuExpandedChange(false)
                            onClearInbox()
                        },
                    )
                }
                if (selectedTab == PodcastTab.UP_NEXT) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.clear_up_next)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        enabled = queueHasItems,
                        onClick = {
                            onMenuExpandedChange(false)
                            showClearQueueConfirmation.value = true
                        },
                    )
                }
                if (selectedTab == PodcastTab.SUBSCRIPTIONS) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_podcast)) },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        onClick = {
                            onMenuExpandedChange(false)
                            onOpenAddPodcast()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.all_episodes)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null) },
                    onClick = {
                        onMenuExpandedChange(false)
                        onOpenAllEpisodes()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.downloads)) },
                    leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                    onClick = {
                        onMenuExpandedChange(false)
                        onOpenDownloads()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.podcast_settings)) },
                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    onClick = {
                        onMenuExpandedChange(false)
                        onOpenSettings()
                    },
                )
            }
        }
    }
    if (showClearQueueConfirmation.value) {
        AlertDialog(
            onDismissRequest = { showClearQueueConfirmation.value = false },
            title = { Text(stringResource(R.string.clear_up_next)) },
            text = { Text(stringResource(R.string.clear_up_next_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearQueueConfirmation.value = false
                        onClearQueue()
                    },
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearQueueConfirmation.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
