package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.EpisodeEntity

internal fun episodeSwipeIndex(episodes: List<EpisodeEntity>, episodeId: Long): Int =
    episodes.indexOfFirst { it.id == episodeId }.coerceAtLeast(0)

@Composable
internal fun PodcastEpisodeSwipePager(
    episodes: List<EpisodeEntity>,
    selectedEpisodeId: Long,
    onSelectEpisode: (EpisodeEntity) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (EpisodeEntity) -> Unit,
) {
    EntitySwipePager(
        items = episodes,
        selectedId = selectedEpisodeId,
        key = { it.id },
        onSelect = onSelectEpisode,
        modifier = modifier,
        content = content,
    )
}
