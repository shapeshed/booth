package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun PodcastHomeTopBarTitle(
    inboxSelectionMode: Boolean,
    selectedInboxCount: Int,
    showPodcastAppSettings: Boolean,
    podcastManagementCategory: PodcastManagementCategory?,
    showDownloads: Boolean,
    showAllEpisodes: Boolean,
    showPodcastSettings: Boolean,
    podcastSettingsTitle: String?,
    showDiscovery: Boolean,
    discoveryTitle: String?,
    hasSelectedPodcast: Boolean,
    hasSelectedEpisode: Boolean,
    selectedTab: PodcastTab,
) {
    when {
        inboxSelectionMode -> Text(selectedInboxCount.toString())
        showPodcastAppSettings -> Text(
            when (podcastManagementCategory) {
                PodcastManagementCategory.PLAYBACK_SPEED -> stringResource(R.string.playback_speed)
                PodcastManagementCategory.AUTO_REFRESH -> stringResource(R.string.auto_refresh)
                PodcastManagementCategory.AUTO_DOWNLOAD -> stringResource(R.string.auto_download)
                PodcastManagementCategory.AUTO_QUEUE -> stringResource(R.string.add_new_episodes_to_up_next)
                PodcastManagementCategory.VIDEO_DOWNLOAD -> stringResource(R.string.download_video)
                PodcastManagementCategory.NOTIFICATIONS -> stringResource(R.string.notifications)
                null -> stringResource(R.string.podcast_settings)
            },
        )
        showDownloads -> Text(stringResource(R.string.downloads))
        showAllEpisodes -> Text(stringResource(R.string.all_episodes))
        showPodcastSettings -> Text(
            podcastSettingsTitle ?: stringResource(R.string.podcast_settings_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        showDiscovery -> Text(discoveryTitle ?: stringResource(R.string.discover))
        !hasSelectedPodcast && !hasSelectedEpisode -> Text(
            when (selectedTab) {
                PodcastTab.HOME -> stringResource(R.string.podcast_inbox)
                PodcastTab.UP_NEXT -> stringResource(R.string.podcast_up_next)
                PodcastTab.SUBSCRIPTIONS -> stringResource(R.string.podcast_subscriptions_tab)
            },
        )
    }
}

@Composable
internal fun PodcastHomeTopBarNavigationIcon(
    inboxSelectionMode: Boolean,
    showPodcastAppSettings: Boolean,
    showDownloads: Boolean,
    showAllEpisodes: Boolean,
    showDiscovery: Boolean,
    hasSelectedPodcast: Boolean,
    hasSelectedEpisode: Boolean,
    onCloseSelection: () -> Unit,
    onBack: () -> Unit,
) {
    when {
        inboxSelectionMode -> IconButton(onClick = onCloseSelection) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.close_selection))
        }
        showPodcastAppSettings || showDownloads || showAllEpisodes || showDiscovery || hasSelectedEpisode || hasSelectedPodcast -> {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        }
    }
}
