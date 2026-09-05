package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.shapeshed.booth.BoothApp
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.DownloadAssetType
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.PodcastDownloadManager
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.PodcastRefreshInterval
import com.shapeshed.booth.data.PodcastRefreshNetwork
import com.shapeshed.booth.data.PodcastSubscriptionsViewMode
import com.shapeshed.booth.data.PodcastSearchProvider
import com.shapeshed.booth.data.relativeTime
import com.shapeshed.booth.data.PodcastRefreshWorker
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    selectedCategory: com.shapeshed.booth.data.PodcastDiscoveryCategory? = null,
    categoryResults: List<com.shapeshed.booth.data.PodcastSearchResult> = emptyList(),
    categoryResultsById: Map<String, List<com.shapeshed.booth.data.PodcastSearchResult>> = emptyMap(),
    isLoadingCategory: Boolean = false,
    onCategorySelected: (com.shapeshed.booth.data.PodcastDiscoveryCategory?) -> Unit = {},
    onPreloadCategory: (com.shapeshed.booth.data.PodcastDiscoveryCategory) -> Unit = {},
    categories: List<com.shapeshed.booth.data.PodcastDiscoveryCategory> = emptyList(),
    onRefresh: () -> Unit,
    refreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val orderedPodcasts = remember(podcasts, sortOrder, latestEpisodePublishedAt) {
        orderPodcasts(podcasts, sortOrder, latestEpisodePublishedAt)
    }
    val listState = rememberLazyListState()
    if (viewMode == PodcastSubscriptionsViewMode.GRID && !showSearch && orderedPodcasts.isNotEmpty()) {
        Column(modifier = modifier) {
            ImportProgressIndicator(importProgress)
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f),
            ) {
                PodcastGridLibrary(
                    podcasts = orderedPodcasts,
                    sortOrder = sortOrder,
                    onSortOrderChange = onSortOrderChange,
                    onViewModeChange = onViewModeChange,
                    onPodcastClick = onPodcastClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        return
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp + LocalPodcastMiniPlayerInset.current),
            verticalArrangement = Arrangement.spacedBy(PodcastListItemSpacing),
        ) {
        if (importProgress != null) {
            item(key = "import-progress") {
                ImportProgressIndicator(importProgress)
            }
        }
        if (showSearch) {
            item(key = "search") {
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
            item(key = "sort-order") {
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
            item(key = "empty") {
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
        items(
            orderedPodcasts,
            key = { "library-${it.id}-${it.subscribedAtMillis}" },
            contentType = { "podcast" },
        ) { podcast ->
            PodcastSwipeRow(
                podcast = podcast,
                onClick = { onPodcastClick(podcast.id) },
                onRemove = { onRemovePodcast(podcast) },
            )
        }
        items(
            state.results,
            key = { "search-${it.podcast.id}" },
            contentType = { "search-result" },
        ) { result ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PodcastActionListItem(
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    leadingContent = {
                        PodcastArtwork(result.podcast.artworkUrl, result.podcast.title, Modifier.size(56.dp))
                    },
                    trailingContent = {
                        Button(onClick = { onSearchResultClick(result.podcast.feedUrl) }) { Text(stringResource(R.string.subscribe)) }
                    },
                ) {
                    Text(result.podcast.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                    Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.grid_view), modifier = Modifier.size(18.dp))
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
                    Icon(Icons.AutoMirrored.Rounded.ViewList, contentDescription = stringResource(R.string.list_view), modifier = Modifier.size(18.dp))
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
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
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
    initiallyShowRemovalConfirmation: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var showRemovalConfirmation by remember { mutableStateOf(initiallyShowRemovalConfirmation) }
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * SwipeToDismissThresholdFraction },
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
                if (state.isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search)) }
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
internal fun PodcastCard(
    podcast: PodcastEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Text(podcast.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            if (showSettings) DropdownMenuItem(
                        text = { Text(stringResource(R.string.podcast_settings_action)) },
                trailingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onSettings()
                },
            )
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
