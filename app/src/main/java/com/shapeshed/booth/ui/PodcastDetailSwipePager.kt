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
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.flow.collectLatest

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
    val currentIndex = remember(podcasts, selectedPodcastId) {
        podcastSwipeIndex(podcasts, selectedPodcastId)
    }
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { podcasts.size },
    )
    // The settled-page collector is keyed on the pager, not on this callback, so it must read the
    // current one rather than whichever instance was captured when it last started.
    val currentOnSelectPodcast by rememberUpdatedState(onSelectPodcast)

    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex && currentIndex < pagerState.pageCount) {
            pagerState.scrollToPage(currentIndex)
        }
    }
    LaunchedEffect(pagerState, podcasts) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            podcasts.getOrNull(page)?.let(currentOnSelectPodcast)
        }
    }

    if (podcasts.size <= 1) {
        // Same shape as the pager below, and for the same reason: the modifier has to reach the
        // container, or the podcast list wrap-sizes inside its pane. This is not a rare path, it is
        // every open from search or now playing, where there is one podcast and nothing to swipe.
        podcasts.firstOrNull()?.let { single ->
            Box(modifier) { content(single) }
        }
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            key = { podcasts[it].id },
        ) { page ->
            Box(Modifier.fillMaxSize()) {
                content(podcasts[page])
            }
        }
    }
}
