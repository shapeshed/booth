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
import kotlinx.coroutines.flow.collectLatest

/**
 * Shared horizontal pager for swiping between entities of the same type.
 *
 * Both the podcast detail pager and the episode detail pager need identical pager behaviour:
 * settle on the initial index, scroll when the selected id changes, and report the settled page.
 * This extracts that logic so the two callers only supply their entity type and key function.
 */
@Composable
internal fun <T> EntitySwipePager(
    items: List<T>,
    selectedId: Long,
    key: (T) -> Long,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val currentIndex = remember(items, selectedId) {
        items.indexOfFirst { key(it) == selectedId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { items.size },
    )
    val currentOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex && currentIndex < pagerState.pageCount) {
            pagerState.scrollToPage(currentIndex)
        }
    }
    LaunchedEffect(pagerState, items) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            items.getOrNull(page)?.let(currentOnSelect)
        }
    }

    // Nothing to show, and a pager with no pages is not useful.
    if (items.isEmpty()) return

    // One call site for `content`, deliberately.
    //
    // This used to branch: `Box(modifier) { content(single) }` for a single item and a
    // `HorizontalPager` otherwise. Invoking the same content slot from two different branches means
    // the slot table can hand the state of one branch's node to the other, so any `remember` inside
    // the content lambda may be reused against the wrong entity. A single page is just a pager with
    // one page and scrolling turned off, which keeps the slot identity stable.
    //
    // `modifier` has to reach this container in the single-item case as well, which is why it is
    // applied here rather than inside the page. It is not a rare path: opening a detail from search
    // or now playing gives one item and nothing to swipe, and a container that is not told to fill
    // its pane makes the list inside it wrap-size instead.
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = items.size > 1,
        key = { items[it].let(key) },
    ) { page ->
        Box(Modifier.fillMaxSize()) {
            content(items[page])
        }
    }
}
