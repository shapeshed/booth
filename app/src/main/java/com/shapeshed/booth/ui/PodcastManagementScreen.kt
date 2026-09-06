package com.shapeshed.booth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.R
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastManagementScreen(
    category: PodcastManagementCategory,
    podcasts: List<PodcastEntity>,
    globalPlaybackSpeed: Float,
    globalSkipSilence: Boolean,
    onPodcastFlagsChange: (PodcastEntity, Boolean, Boolean, Boolean) -> Unit,
    onPodcastVideoDownloadChange: (PodcastEntity, Boolean) -> Unit,
    onPodcastAutoQueueChange: (PodcastEntity, Boolean) -> Unit,
    onPodcastSpeedChange: (PodcastEntity, Float?) -> Unit,
    onPodcastSkipSilenceChange: (PodcastEntity, Boolean?) -> Unit,
    onSetAllEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingPodcastId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingPodcast = podcasts.firstOrNull { it.id == editingPodcastId }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 24.dp + LocalPodcastMiniPlayerInset.current,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (category != PodcastManagementCategory.PLAYBACK_SPEED) {
            item {
                val allEnabled = podcasts.isNotEmpty() && podcasts.all {
                    when (category) {
                        PodcastManagementCategory.AUTO_REFRESH -> it.includeInAutoRefresh
                        PodcastManagementCategory.AUTO_DOWNLOAD -> it.includeInAutoDownload
                        PodcastManagementCategory.AUTO_QUEUE -> it.includeInAutoQueue
                        PodcastManagementCategory.VIDEO_DOWNLOAD -> it.includeInVideoDownload
                        PodcastManagementCategory.NOTIFICATIONS -> it.includeInNotifications
                        PodcastManagementCategory.PLAYBACK_SPEED -> false
                    }
                }
                ListItem(
                    trailingContent = {
                        Switch(
                            checked = allEnabled,
                            onCheckedChange = onSetAllEnabled,
                        )
                    },
                ) { Text(stringResource(R.string.enable_all_podcasts)) }
            }
        }
        items(podcasts, key = PodcastEntity::id) { podcast ->
            when (category) {
                PodcastManagementCategory.PLAYBACK_SPEED -> PodcastSpeedItem(
                    podcast = podcast,
                    globalPlaybackSpeed = globalPlaybackSpeed,
                    onClick = { editingPodcastId = podcast.id },
                )
                PodcastManagementCategory.AUTO_REFRESH -> PodcastToggleItem(
                    podcast = podcast,
                    checked = podcast.includeInAutoRefresh,
                    onCheckedChange = { onPodcastFlagsChange(podcast, it, podcast.includeInAutoDownload, podcast.includeInNotifications) },
                )
                PodcastManagementCategory.AUTO_DOWNLOAD -> PodcastToggleItem(
                    podcast = podcast,
                    checked = podcast.includeInAutoDownload,
                    onCheckedChange = { onPodcastFlagsChange(podcast, podcast.includeInAutoRefresh, it, podcast.includeInNotifications) },
                )
                PodcastManagementCategory.AUTO_QUEUE -> PodcastToggleItem(
                    podcast = podcast,
                    checked = podcast.includeInAutoQueue,
                    onCheckedChange = { onPodcastAutoQueueChange(podcast, it) },
                )
                PodcastManagementCategory.VIDEO_DOWNLOAD -> PodcastToggleItem(
                    podcast = podcast,
                    checked = podcast.includeInVideoDownload,
                    onCheckedChange = { onPodcastVideoDownloadChange(podcast, it) },
                )
                PodcastManagementCategory.NOTIFICATIONS -> PodcastToggleItem(
                    podcast = podcast,
                    checked = podcast.includeInNotifications,
                    onCheckedChange = { onPodcastFlagsChange(podcast, podcast.includeInAutoRefresh, podcast.includeInAutoDownload, it) },
                )
            }
        }
    }
    editingPodcast?.let { podcast ->
        PlaybackSpeedSheet(
            speed = podcast.playbackSpeed ?: globalPlaybackSpeed,
            skipSilence = podcast.skipSilence ?: globalSkipSilence,
            onSpeedChange = { onPodcastSpeedChange(podcast, it); editingPodcastId = null },
            onSkipSilenceChange = { onPodcastSkipSilenceChange(podcast, it) },
            onUseGlobal = { onPodcastSpeedChange(podcast, null); editingPodcastId = null },
            onDismiss = { editingPodcastId = null },
            inherited = podcast.playbackSpeed == null,
            globalSpeed = globalPlaybackSpeed,
        )
    }
}

@Composable
private fun PodcastSpeedItem(
    podcast: PodcastEntity,
    globalPlaybackSpeed: Float,
    onClick: () -> Unit,
) {
    ListItem(
        supportingContent = {
            Text(
                podcast.playbackSpeed?.let { formatPlaybackSpeed(it) }
                    ?: stringResource(R.string.inherited_global_playback_speed, formatPlaybackSpeed(globalPlaybackSpeed)),
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    ) { Text(podcast.title, maxLines = 2) }
}

@Composable
private fun PodcastToggleItem(
    podcast: PodcastEntity,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    ) { Text(podcast.title, maxLines = 2) }
}
