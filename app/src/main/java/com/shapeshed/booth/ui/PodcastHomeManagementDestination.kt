package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.podcastTags

/**
 * The podcast management screen's actions.
 *
 * The screen is a set of toggles over a list of podcasts, and every one of them writes through the
 * ViewModel. Taking the ViewModel here made the screen impossible to render without Hilt.
 *
 * [updateFlags] takes the podcast and the three booleans rather than the eight arguments the
 * ViewModel method takes, because the remaining five are derived from the podcast and deriving them
 * here would mean the screen has to know how tags and skip markers are built.
 *
 * The "set all" actions are separate entries rather than one callback taking the category, so that
 * picking the right one stays the screen's decision. It is the screen that knows which category is
 * open.
 */
internal class PodcastManagementActions(
    val updateFlags: (
        podcast: PodcastEntity,
        refresh: Boolean,
        download: Boolean,
        notifications: Boolean,
    ) -> Unit,
    val setAutoQueue: (podcastId: Long, enabled: Boolean) -> Unit,
    val setAllAutoRefresh: (enabled: Boolean) -> Unit,
    val setAllAutoQueue: (enabled: Boolean) -> Unit,
    val setAllNotifications: (enabled: Boolean) -> Unit,
)

/**
 * Builds the management actions where the ViewModel is in scope.
 *
 * Keyed on the ViewModel alone, which is sound because [PodcastManagementActions] captures no caller
 * lambda.
 */
@Composable
internal fun rememberManagementActions(viewModel: PodcastViewModel): PodcastManagementActions = remember(viewModel) {
    PodcastManagementActions(
        updateFlags = { podcast, refresh, download, notifications ->
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
        setAutoQueue = { podcastId, enabled -> viewModel.setPodcastAutoQueue(podcastId, enabled) },
        setAllAutoRefresh = { enabled -> viewModel.setAllPodcastAutoRefresh(enabled) },
        setAllAutoQueue = { enabled -> viewModel.setAllPodcastAutoQueue(enabled) },
        setAllNotifications = { enabled -> viewModel.setAllPodcastNotifications(enabled) },
    )
}

@Composable
internal fun PodcastHomeManagementDestination(
    destination: PodcastNavigationKey.PodcastManagement,
    podcasts: List<PodcastEntity>,
    actions: PodcastManagementActions,
    modifier: Modifier = Modifier,
) {
    PodcastManagementScreen(
        category = destination.category,
        podcasts = podcasts,
        onPodcastFlagsChange = { podcast, refresh, download, notifications ->
            actions.updateFlags(podcast, refresh, download, notifications)
        },
        onPodcastAutoQueueChange = { podcast, enabled ->
            actions.setAutoQueue(podcast.id, enabled)
        },
        onSetAllEnabled = { enabled ->
            when (destination.category) {
                PodcastManagementCategory.AUTO_REFRESH -> actions.setAllAutoRefresh(enabled)
                PodcastManagementCategory.AUTO_QUEUE -> actions.setAllAutoQueue(enabled)
                PodcastManagementCategory.NOTIFICATIONS -> actions.setAllNotifications(enabled)
            }
        },
        modifier = modifier,
    )
}
