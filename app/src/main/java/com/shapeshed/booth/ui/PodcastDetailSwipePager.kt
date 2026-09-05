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
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.flow.collectLatest

internal fun podcastSwipeIndex(podcasts: List<PodcastEntity>, podcastId: Long): Int =
    podcasts.indexOfFirst { it.id == podcastId }.coerceAtLeast(0)

@Composable
internal fun PodcastDetailSwipePager(
    podcasts: List<PodcastEntity>,
    selectedPodcastId: Long,
    onPodcastSelected: (PodcastEntity) -> Unit,
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

    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex && currentIndex < pagerState.pageCount) {
            pagerState.scrollToPage(currentIndex)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            podcasts.getOrNull(page)?.let(onPodcastSelected)
        }
    }

    if (podcasts.size <= 1) {
        if (podcasts.isNotEmpty()) content(podcasts[0])
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
