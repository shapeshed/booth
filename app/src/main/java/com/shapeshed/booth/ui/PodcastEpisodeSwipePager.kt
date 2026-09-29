package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    onSelectEpisode: (EpisodeEntity) -> Unit,
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
    // The settled-page collector is keyed on the pager, not on this callback, so it must read the
    // current one rather than whichever instance was captured when it last started.
    val currentOnSelectEpisode by rememberUpdatedState(onSelectEpisode)

    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex && currentIndex < pagerState.pageCount) {
            pagerState.scrollToPage(currentIndex)
        }
    }
    LaunchedEffect(pagerState, episodes) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            episodes.getOrNull(page)?.let(currentOnSelectEpisode)
        }
    }

    if (episodes.size <= 1) {
        // Same shape as the pager below, and for the same reason: the modifier has to reach the
        // container, or the episode list wrap-sizes inside its pane. This is not a rare path, it is
        // every open from search or now playing, where there is one episode and nothing to swipe.
        episodes.firstOrNull()?.let { single ->
            Box(modifier) { content(single) }
        }
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
