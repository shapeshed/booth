package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastSettingsContent(
    podcast: PodcastEntity,
    globalPlaybackSpeed: Float,
    onPlaybackSpeedChange: (Float?) -> Unit,
    videoDownloadsEnabled: Boolean,
    onVideoDownloadsEnabledChange: (Boolean) -> Unit,
    globalAutoRefreshEnabled: Boolean,
    globalAutoDownloadEnabled: Boolean,
    globalAutoQueueEnabled: Boolean,
    globalNotificationsEnabled: Boolean,
    availableTags: List<String>,
    onSaveSettings: (String, Int, Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    PodcastSettingsScreen(
        podcast = podcast,
        globalPlaybackSpeed = globalPlaybackSpeed,
        onPlaybackSpeedChange = onPlaybackSpeedChange,
        videoDownloadsEnabled = videoDownloadsEnabled,
        onVideoDownloadsEnabledChange = onVideoDownloadsEnabledChange,
        globalAutoRefreshEnabled = globalAutoRefreshEnabled,
        globalAutoDownloadEnabled = globalAutoDownloadEnabled,
        globalAutoQueueEnabled = globalAutoQueueEnabled,
        globalNotificationsEnabled = globalNotificationsEnabled,
        availableTags = availableTags,
        onSaveSettings = onSaveSettings,
        modifier = modifier,
    )
}
