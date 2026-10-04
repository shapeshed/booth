package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.PodcastEntity

internal fun podcastSwipeIndex(podcasts: List<PodcastEntity>, podcastId: Long): Int =
    podcasts.indexOfFirst { it.id == podcastId }.coerceAtLeast(0)

@Composable
internal fun PodcastDetailSwipePager(
    podcasts: List<PodcastEntity>,
    selectedPodcastId: Long,
    onSelectPodcast: (PodcastEntity) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PodcastEntity) -> Unit,
) {
    EntitySwipePager(
        items = podcasts,
        selectedId = selectedPodcastId,
        key = { it.id },
        onSelect = onSelectPodcast,
        modifier = modifier,
        content = content,
    )
}
