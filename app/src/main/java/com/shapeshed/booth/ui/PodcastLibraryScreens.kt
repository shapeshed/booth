package com.shapeshed.booth.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.R
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.PodcastSubscriptionsViewMode

@Composable
internal fun PodcastLibrary(
    podcasts: List<PodcastEntity>,
    latestEpisodePublishedAt: Map<Long, Long?>,
    viewMode: PodcastSubscriptionsViewMode,
    onViewModeChange: (PodcastSubscriptionsViewMode) -> Unit,
    sortOrder: PodcastSortOrder,
    onSortOrderChange: (PodcastSortOrder) -> Unit,
    state: PodcastHomeState,
    showSearch: Boolean,
    directFeedUrl: String,
    onDirectFeedUrlChange: (String) -> Unit,
    onSubscribeDirect: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPodcastClick: (Long) -> Unit,
    onRemovePodcast: (PodcastEntity) -> Unit,
    onSearchResultClick: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onImportOpml: () -> Unit,
    importProgress: PodcastImportProgress?,
    popularPodcasts: List<com.shapeshed.booth.data.PodcastSearchResult>,
    isLoadingPopular: Boolean,
    onOpenPopularPodcast: (com.shapeshed.booth.data.PodcastSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    selectedCategory: com.shapeshed.booth.data.PodcastDiscoveryCategory? = null,
    categoryResults: List<com.shapeshed.booth.data.PodcastSearchResult> = emptyList(),
    categoryResultsById: Map<String, List<com.shapeshed.booth.data.PodcastSearchResult>> = emptyMap(),
    isLoadingCategory: Boolean = false,
    onCategorySelected: (com.shapeshed.booth.data.PodcastDiscoveryCategory?) -> Unit = {},
    onPreloadCategory: (com.shapeshed.booth.data.PodcastDiscoveryCategory) -> Unit = {},
    categories: List<com.shapeshed.booth.data.PodcastDiscoveryCategory> = emptyList(),
    onRefresh: () -> Unit,
    refreshing: Boolean,
) {
    val orderedPodcasts = remember(podcasts, sortOrder, latestEpisodePublishedAt) {
        orderPodcasts(podcasts, sortOrder, latestEpisodePublishedAt)
    }
    val gridState = rememberLazyGridState()
    val cards = viewMode == PodcastSubscriptionsViewMode.GRID && !showSearch && orderedPodcasts.isNotEmpty()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyVerticalGrid(
            columns = if (cards) GridCells.Adaptive(minSize = 128.dp) else GridCells.Fixed(1),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = if (cards) Arrangement.spacedBy(16.dp) else Arrangement.Start,
            verticalArrangement = if (cards) {
                Arrangement.spacedBy(
                    16.dp,
                )
            } else {
                Arrangement.spacedBy(PODCAST_LIST_ITEM_SPACING)
            },
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (cards) 8.dp else 0.dp,
                bottom = 16.dp + LocalPodcastMiniPlayerInset.current,
            ),
        ) {
            if (importProgress != null) {
                item(key = "import-progress", span = {
                    androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)
                }) {
                    ImportProgressIndicator(importProgress)
                }
            }
            if (showSearch) {
                item(key = "search", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    SearchPanel(
                        state = state,
                        directFeedUrl = directFeedUrl,
                        onDirectFeedUrlChange = onDirectFeedUrlChange,
                        onSubscribeDirect = onSubscribeDirect,
                        onQueryChange = onQueryChange,
                        onSearch = onSearch,
                    )
                }
            }
            if (!showSearch && orderedPodcasts.isNotEmpty()) {
                item(key = "sort-order", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    PodcastLibraryControls(
                        sortOrder = sortOrder,
                        onSortOrderChange = onSortOrderChange,
                        viewMode = viewMode,
                        onViewModeChange = onViewModeChange,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            if (podcasts.isEmpty() && !showSearch) {
                item(key = "empty", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    PodcastGettingStarted(
                        popularPodcasts = popularPodcasts,
                        isLoadingPopular = isLoadingPopular,
                        onSearch = onOpenSearch,
                        onImportOpml = onImportOpml,
                        onOpenPodcast = onOpenPopularPodcast,
                        selectedCategory = selectedCategory,
                        categoryResults = categoryResults,
                        categoryResultsById = categoryResultsById,
                        isLoadingCategory = isLoadingCategory,
                        onCategorySelected = onCategorySelected,
                        onPreloadCategory = onPreloadCategory,
                        categories = categories,
                    )
                }
            }
            gridItems(
                orderedPodcasts,
                key = { "library-${it.id}-${it.subscribedAtMillis}" },
                contentType = { "podcast" },
            ) { podcast ->
                Box(modifier = Modifier.animateItem()) {
                    if (cards) {
                        PodcastGridCard(
                            artworkUrl = podcast.artworkUrl,
                            title = podcast.title,
                            onClick = { onPodcastClick(podcast.id) },
                        )
                    } else {
                        PodcastSwipeRow(
                            podcast = podcast,
                            onClick = { onPodcastClick(podcast.id) },
                            onRemove = { onRemovePodcast(podcast) },
                        )
                    }
                }
            }
            gridItems(
                state.results,
                key = { "search-${it.podcast.id}" },
                contentType = { "search-result" },
            ) { result ->
                Box(modifier = Modifier.fillMaxWidth().animateItem()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PodcastActionListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                            leadingContent = {
                                PodcastArtwork(result.podcast.artworkUrl, result.podcast.title, Modifier.size(56.dp))
                            },
                            trailingContent = {
                                Button(onClick = {
                                    onSearchResultClick(result.podcast.feedUrl)
                                }) { Text(stringResource(R.string.subscribe)) }
                            },
                        ) {
                            Text(
                                result.podcast.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportProgressIndicator(progress: PodcastImportProgress?) {
    if (progress == null) return
    LinearProgressIndicator(
        progress = {
            progress.completed.toFloat() / progress.total.coerceAtLeast(1)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

internal fun orderPodcasts(
    podcasts: List<PodcastEntity>,
    sortOrder: PodcastSortOrder,
    latestEpisodePublishedAt: Map<Long, Long?>,
): List<PodcastEntity> = when (sortOrder) {
    PodcastSortOrder.LAST_UPDATED -> podcasts.sortedWith(
        compareByDescending<PodcastEntity> {
            latestEpisodePublishedAt[it.id] ?: Long.MIN_VALUE
        }.thenBy { it.title.lowercase() },
    )

    PodcastSortOrder.A_TO_Z -> podcasts.sortedBy { it.title.lowercase() }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun PodcastLibraryControls(
    sortOrder: PodcastSortOrder,
    onSortOrderChange: (PodcastSortOrder) -> Unit,
    viewMode: PodcastSubscriptionsViewMode,
    onViewModeChange: (PodcastSubscriptionsViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = sortOrder == PodcastSortOrder.LAST_UPDATED,
                onClick = { onSortOrderChange(PodcastSortOrder.LAST_UPDATED) },
                label = { Text(stringResource(R.string.last_updated)) },
            )
            FilterChip(
                selected = sortOrder == PodcastSortOrder.A_TO_Z,
                onClick = { onSortOrderChange(PodcastSortOrder.A_TO_Z) },
                label = { Text(stringResource(R.string.feed_sort_az)) },
            )
        }
        Spacer(Modifier.size(8.dp))
        PodcastViewModeToggle(
            selected = viewMode,
            onSelected = onViewModeChange,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun PodcastViewModeToggle(
    selected: PodcastSubscriptionsViewMode,
    onSelected: (PodcastSubscriptionsViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridIconScale by animateFloatAsState(
        targetValue = if (selected == PodcastSubscriptionsViewMode.GRID) 1f else 0.82f,
        animationSpec = tween(180),
        label = "grid view icon scale",
    )
    val listIconScale by animateFloatAsState(
        targetValue = if (selected == PodcastSubscriptionsViewMode.LIST) 1f else 0.82f,
        animationSpec = tween(180),
        label = "list view icon scale",
    )
    ButtonGroup(
        overflowIndicator = {},
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = modifier,
    ) {
        customItem(
            buttonGroupContent = {
                ToggleButton(
                    checked = selected == PodcastSubscriptionsViewMode.GRID,
                    onCheckedChange = { if (it) onSelected(PodcastSubscriptionsViewMode.GRID) },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                ) {
                    Icon(
                        Icons.Rounded.GridView,
                        contentDescription = stringResource(R.string.grid_view),
                        modifier = Modifier.size(18.dp).graphicsLayer {
                            scaleX = gridIconScale
                            scaleY = gridIconScale
                        },
                    )
                }
            },
            menuContent = {},
        )
        customItem(
            buttonGroupContent = {
                ToggleButton(
                    checked = selected == PodcastSubscriptionsViewMode.LIST,
                    onCheckedChange = { if (it) onSelected(PodcastSubscriptionsViewMode.LIST) },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = stringResource(R.string.list_view),
                        modifier = Modifier.size(18.dp).graphicsLayer {
                            scaleX = listIconScale
                            scaleY = listIconScale
                        },
                    )
                }
            },
            menuContent = {},
        )
    }
}

@Composable
internal fun PodcastGridLibrary(
    podcasts: List<PodcastEntity>,
    sortOrder: PodcastSortOrder,
    onSortOrderChange: (PodcastSortOrder) -> Unit,
    onViewModeChange: (PodcastSubscriptionsViewMode) -> Unit,
    onPodcastClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 16.dp + LocalPodcastMiniPlayerInset.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "grid-controls", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            PodcastLibraryControls(
                sortOrder = sortOrder,
                onSortOrderChange = onSortOrderChange,
                viewMode = PodcastSubscriptionsViewMode.GRID,
                onViewModeChange = onViewModeChange,
            )
        }
        gridItems(
            podcasts,
            key = { "grid-podcast-${it.id}" },
            contentType = { "podcast" },
        ) { podcast ->
            PodcastGridCard(
                artworkUrl = podcast.artworkUrl,
                title = podcast.title,
                onClick = { onPodcastClick(podcast.id) },
            )
        }
    }
}

@Composable
internal fun PodcastSwipeRow(
    podcast: PodcastEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyShowRemovalConfirmation: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    var showRemovalConfirmation by remember { mutableStateOf(initiallyShowRemovalConfirmation) }
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * SWIPE_TO_DISMISS_THRESHOLD_FRACTION },
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            showRemovalConfirmation = true
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .semantics { contentDescription = "Podcast swipe row ${podcast.title}" },
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
            ) {
                if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.remove_podcast, podcast.title),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .align(
                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                    Alignment.CenterStart
                                } else {
                                    Alignment.CenterEnd
                                },
                            )
                            .padding(16.dp),
                    )
                }
            }
        },
    ) {
        PodcastCard(podcast = podcast, onClick = onClick)
    }
    if (showRemovalConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemovalConfirmation = false },
            title = { Text(stringResource(R.string.remove_podcast, podcast.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemovalConfirmation = false
                        onRemove()
                    },
                ) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemovalConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
internal fun SearchPanel(
    state: PodcastHomeState,
    directFeedUrl: String,
    onDirectFeedUrlChange: (String) -> Unit,
    onSubscribeDirect: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    showDirectFeed: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_feeds)) },
            singleLine = true,
            trailingIcon = {
                if (state.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
                    }
                }
            },
        )
        state.error?.let { Text(podcastErrorMessage(it), color = MaterialTheme.colorScheme.error) }
        if (showDirectFeed) {
            OutlinedTextField(
                value = directFeedUrl,
                onValueChange = onDirectFeedUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rss_feed_url)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onSubscribeDirect, enabled = directFeedUrl.isNotBlank()) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.subscribe_to_rss_feed))
                    }
                },
            )
        }
    }
}

@Composable
internal fun PodcastCard(podcast: PodcastEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        PodcastActionListItem(
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            leadingContent = {
                PodcastArtwork(
                    imageUrl = podcast.artworkUrl,
                    title = podcast.title,
                    modifier = Modifier.size(56.dp),
                )
            },
            supportingContent = podcast.author?.let { author ->
                { Text(author, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            },
        ) {
            Text(
                podcast.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PodcastMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    showSettings: Boolean = true,
    onSettings: () -> Unit,
    onUnsubscribe: () -> Unit,
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.podcast_options))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            if (showSettings) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.podcast_settings_action)) },
                    trailingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    onClick = {
                        onExpandedChange(false)
                        onSettings()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.unfollow)) },
                trailingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onUnsubscribe()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                trailingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onShare()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.open_in_browser)) },
                trailingIcon = { Icon(Icons.Rounded.OpenInBrowser, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onOpenInBrowser()
                },
            )
        }
    }
}
