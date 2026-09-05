package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.EpisodeEntity
import kotlinx.coroutines.flow.collectLatest

internal fun episodeSwipeIndex(episodes: List<EpisodeEntity>, episodeId: Long): Int =
    episodes.indexOfFirst { it.id == episodeId }.coerceAtLeast(0)

@Composable
internal fun PodcastEpisodeSwipePager(
    episodes: List<EpisodeEntity>,
    selectedEpisodeId: Long,
    onEpisodeSelected: (EpisodeEntity) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (EpisodeEntity) -> Unit,
) {
    val currentIndex = remember(episodes, selectedEpisodeId) {
        episodeSwipeIndex(episodes, selectedEpisodeId)
    }
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { episodes.size },
    )

    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex && currentIndex < pagerState.pageCount) {
            pagerState.scrollToPage(currentIndex)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            episodes.getOrNull(page)?.let(onEpisodeSelected)
        }
    }

    if (episodes.size <= 1) {
        episodes.firstOrNull()?.let { content(it) }
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            key = { episodes[it].id },
        ) { page ->
            Box(Modifier.fillMaxSize()) {
                content(episodes[page])
            }
        }
    }
}
