package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.R
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastManagementScreen(
    category: PodcastManagementCategory,
    podcasts: List<PodcastEntity>,
    onPodcastFlagsChange: (PodcastEntity, Boolean, Boolean, Boolean) -> Unit,
    onPodcastAutoQueueChange: (PodcastEntity, Boolean) -> Unit,
    onSetAllEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        item {
            val allEnabled = podcasts.isNotEmpty() && podcasts.all {
                when (category) {
                    PodcastManagementCategory.AUTO_REFRESH -> it.includeInAutoRefresh
                    PodcastManagementCategory.AUTO_QUEUE -> it.includeInAutoQueue
                    PodcastManagementCategory.NOTIFICATIONS -> it.includeInNotifications
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
        items(podcasts, key = PodcastEntity::id) { podcast ->
            when (category) {
                PodcastManagementCategory.AUTO_REFRESH -> PodcastToggleItem(
                    podcast = podcast,
                    checked = podcast.includeInAutoRefresh,
                    onCheckedChange = {
                        onPodcastFlagsChange(podcast, it, podcast.includeInAutoDownload, podcast.includeInNotifications)
                    },
                )

                PodcastManagementCategory.AUTO_QUEUE -> PodcastToggleItem(
                    podcast = podcast,
                    checked = podcast.includeInAutoQueue,
                    onCheckedChange = { onPodcastAutoQueueChange(podcast, it) },
                )

                PodcastManagementCategory.NOTIFICATIONS -> PodcastToggleItem(
                    podcast = podcast,
                    checked = podcast.includeInNotifications,
                    onCheckedChange = {
                        onPodcastFlagsChange(podcast, podcast.includeInAutoRefresh, podcast.includeInAutoDownload, it)
                    },
                )
            }
        }
    }
}

@Composable
private fun PodcastToggleItem(podcast: PodcastEntity, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    ) { Text(podcast.title, maxLines = 2) }
}
