package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.podcastTags

@Composable
internal fun PodcastHomeManagementDestination(
    destination: PodcastNavigationKey.PodcastManagement,
    podcasts: List<PodcastEntity>,
    viewModel: PodcastViewModel,
    modifier: Modifier = Modifier,
) {
    PodcastManagementScreen(
        category = destination.category,
        podcasts = podcasts,
        onPodcastFlagsChange = { podcast, refresh, download, notifications ->
            viewModel.updatePodcastSettings(
                podcast.id,
                podcastTags(listOf(podcast)).joinToString(", "),
                podcast.skipStartSeconds,
                podcast.skipEndSeconds,
                refresh,
                download,
                podcast.includeInAutoQueue,
                notifications,
            )
        },
        onPodcastAutoQueueChange = { podcast, enabled ->
            viewModel.setPodcastAutoQueue(podcast.id, enabled)
        },
        onSetAllEnabled = { enabled ->
            when (destination.category) {
                PodcastManagementCategory.AUTO_REFRESH -> viewModel.setAllPodcastAutoRefresh(enabled)
                PodcastManagementCategory.AUTO_QUEUE -> viewModel.setAllPodcastAutoQueue(enabled)
                PodcastManagementCategory.NOTIFICATIONS -> viewModel.setAllPodcastNotifications(enabled)
            }
        },
        modifier = modifier,
    )
}
