package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared scrolling and refresh shell for local and discovery podcast detail pages. */
@Composable
internal fun PodcastDetailTemplate(
    refreshing: Boolean,
    onRefresh: (() -> Unit)?,
    state: LazyListState? = null,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val list = @Composable {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state ?: androidx.compose.foundation.lazy.rememberLazyListState(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 16.dp + LocalPodcastMiniPlayerInset.current,
            ),
            verticalArrangement = Arrangement.spacedBy(PodcastListItemSpacing),
            content = content,
        )
    }
    if (onRefresh != null) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            content = { list() },
        )
    } else {
        Box(modifier = modifier) { list() }
    }
}
