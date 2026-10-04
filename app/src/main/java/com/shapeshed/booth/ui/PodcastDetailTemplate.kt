package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.ui.theme.Spacing

/** Shared scrolling and refresh shell for local and discovery podcast detail pages. */
@Composable
internal fun PodcastDetailTemplate(
    refreshing: Boolean,
    onRefresh: (() -> Unit)?,
    modifier: Modifier = Modifier,
    state: LazyListState? = null,
    content: LazyListScope.() -> Unit,
) {
    val list = @Composable {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state ?: androidx.compose.foundation.lazy.rememberLazyListState(),
            contentPadding = PaddingValues(
                start = Spacing.screenInset,
                end = Spacing.screenInset,
                top = Spacing.listTop,
                bottom = Spacing.miniPlayer + LocalPodcastMiniPlayerInset.current,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.listItem),
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
