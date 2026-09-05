package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.R
import com.shapeshed.booth.data.PodcastDiscoveryCategories
import com.shapeshed.booth.data.PodcastDiscoveryCategory
import com.shapeshed.booth.data.PodcastSearchResult
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun PodcastGettingStarted(
    popularPodcasts: List<PodcastSearchResult>,
    isLoadingPopular: Boolean,
    onSearch: () -> Unit,
    onImportOpml: () -> Unit,
    onOpenPodcast: (PodcastSearchResult) -> Unit,
    selectedCategory: PodcastDiscoveryCategory? = null,
    categoryResults: List<PodcastSearchResult> = emptyList(),
    categoryResultsById: Map<String, List<PodcastSearchResult>> = emptyMap(),
    isLoadingCategory: Boolean = false,
    onCategorySelected: (PodcastDiscoveryCategory?) -> Unit = {},
    onPreloadCategory: (PodcastDiscoveryCategory) -> Unit = {},
    categories: List<PodcastDiscoveryCategory> = PodcastDiscoveryCategories,
    modifier: Modifier = Modifier,
) {
    val selectedPage = selectedCategory?.let { category ->
        categories.indexOfFirst { it.id == category.id } + 1
    } ?: 0
    val pagerState = rememberPagerState(initialPage = selectedPage) { categories.size + 1 }
    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }
    LaunchedEffect(pagerState, categories) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page -> onCategorySelected(categories.getOrNull(page - 1)) }
    }
    LaunchedEffect(pagerState, categories) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                listOf(page - 1, page + 1)
                    .mapNotNull { categories.getOrNull(it - 1) }
                    .distinctBy(PodcastDiscoveryCategory::id)
                    .forEach(onPreloadCategory)
            }
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isTablet = maxWidth >= 600.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isTablet) 960.dp else 840.dp)
                .padding(horizontal = if (isTablet) 32.dp else 16.dp, vertical = if (isTablet) 40.dp else 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Text(
            text = stringResource(R.string.get_started_title),
            style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.get_started_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isTablet) 640.dp else 840.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onSearch,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.search_podcasts))
            }
            OutlinedButton(
                onClick = onImportOpml,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.import_opml))
            }
        }
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            edgePadding = 0.dp,
        ) {
            Tab(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                text = { Text(stringResource(R.string.popular_podcasts)) },
            )
            categories.forEach { category ->
                Tab(
                    selected = selectedCategory?.id == category.id,
                    onClick = { onCategorySelected(category) },
                    text = { Text(category.title) },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp, max = 560.dp),
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1,
        ) { page ->
            val pagePodcasts = if (page == 0) {
                popularPodcasts
            } else if (selectedCategory?.id == categories.getOrNull(page - 1)?.id) {
                categoryResults
            } else {
                categoryResultsById[categories.getOrNull(page - 1)?.id].orEmpty()
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (pagePodcasts.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (isTablet) 160.dp else 96.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        gridItems(pagePodcasts, key = { it.podcast.feedUrl }) { result ->
                            PodcastGridCard(
                                artworkUrl = result.podcast.artworkUrl,
                                title = result.podcast.title,
                                onClick = { onOpenPodcast(result) },
                            )
                        }
                    }
                }
                val pageIsLoading = if (page == 0) {
                    isLoadingPopular
                } else {
                    selectedCategory?.id == categories.getOrNull(page - 1)?.id && isLoadingCategory
                }
                if (pageIsLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        }
    }
}
