package com.shapeshed.booth.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.material3.AppBarWithSearch
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.Episode
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.DownloadAssetType
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.PodcastDownloadManager
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.searchableCategories
import com.shapeshed.booth.data.displayCategories
import com.shapeshed.booth.data.podcastTags
import com.shapeshed.booth.data.APPLE_DIRECTORY_PROVIDER_ID
import com.shapeshed.booth.data.PodcastRefreshInterval
import com.shapeshed.booth.data.PodcastRefreshNetwork
import com.shapeshed.booth.data.PodcastSubscriptionsViewMode
import com.shapeshed.booth.data.PodcastSearchProvider
import com.shapeshed.booth.data.PodcastSearchResult
import com.shapeshed.booth.data.PodcastEpisodeSearchResult
import com.shapeshed.booth.data.PodcastDiscoveryCategory
import com.shapeshed.booth.data.PodcastDiscoveryCategories
import com.shapeshed.booth.data.PodcastSubscriptionProgressStore
import com.shapeshed.booth.data.isInProgress
import com.shapeshed.booth.data.isAdded
import com.shapeshed.booth.data.PodcastSubscriptionStage
import com.shapeshed.booth.data.canonicalFeedUrl
import com.shapeshed.booth.data.relativeTime
import com.shapeshed.booth.data.PodcastRefreshWorker
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

internal enum class PodcastSortOrder { LAST_UPDATED, A_TO_Z }

internal enum class QueueFilter { ALL, UNPLAYED, IN_PROGRESS, COMPLETED }

internal enum class DownloadsSortOrder {
    DATE_NEWEST,
    DATE_OLDEST,
    SIZE_LARGEST,
    SIZE_SMALLEST,
    STATE_DOWNLOADED,
    STATE_ACTIVE,
    STATE_FAILED,
}

internal enum class PodcastTab { HOME, UP_NEXT, SUBSCRIPTIONS }

internal enum class EpisodeOrigin { INBOX, UP_NEXT, DOWNLOADS, ALL_EPISODES, PODCAST, SEARCH }

internal sealed interface PodcastEpisodeAction {
    data class Subscribed(
        val episode: EpisodeEntity,
        val podcastTitle: String,
        val podcastArtworkUrl: String?,
        val linkUrl: String?,
        val isDownloaded: Boolean,
        val downloadSizeBytes: Long?,
        val isCompleted: Boolean,
        val isInQueue: Boolean,
        val hasPlaybackPosition: Boolean,
    ) : PodcastEpisodeAction
    data class Preview(
        val episode: com.shapeshed.booth.data.Episode,
        val podcast: com.shapeshed.booth.data.Podcast,
        val podcastTitle: String,
        val podcastArtworkUrl: String?,
        val linkUrl: String?,
        val isDownloaded: Boolean,
        val downloadSizeBytes: Long?,
        val isCompleted: Boolean,
        val isInQueue: Boolean,
        val hasPlaybackPosition: Boolean,
    ) : PodcastEpisodeAction
}

internal val PodcastEpisodeAction.episodeTitle: String
    get() = when (this) {
        is PodcastEpisodeAction.Subscribed -> episode.title
        is PodcastEpisodeAction.Preview -> episode.title
    }

internal val PodcastEpisodeAction.linkUrl: String?
    get() = when (this) {
        is PodcastEpisodeAction.Subscribed -> linkUrl
        is PodcastEpisodeAction.Preview -> linkUrl
    }

internal val PodcastEpisodeAction.isDownloaded: Boolean
    get() = when (this) {
        is PodcastEpisodeAction.Subscribed -> isDownloaded
        is PodcastEpisodeAction.Preview -> isDownloaded
    }

internal val PodcastEpisodeAction.downloadSizeBytes: Long?
    get() = when (this) {
        is PodcastEpisodeAction.Subscribed -> downloadSizeBytes
        is PodcastEpisodeAction.Preview -> downloadSizeBytes
    }

internal val PodcastEpisodeAction.isCompleted: Boolean
    get() = when (this) {
        is PodcastEpisodeAction.Subscribed -> isCompleted
        is PodcastEpisodeAction.Preview -> isCompleted
    }

internal val PodcastEpisodeAction.isInQueue: Boolean
    get() = when (this) {
        is PodcastEpisodeAction.Subscribed -> isInQueue
        is PodcastEpisodeAction.Preview -> isInQueue
    }

internal val PodcastEpisodeAction.hasPlaybackPosition: Boolean
    get() = when (this) {
        is PodcastEpisodeAction.Subscribed -> hasPlaybackPosition
        is PodcastEpisodeAction.Preview -> hasPlaybackPosition
    }

// Material's 8dp spacing rhythm for adjacent card-like list items.
internal val PodcastListItemSpacing = 8.dp
internal val PodcastEpisodeArtworkSize = 80.dp
// Require an intentional horizontal gesture so vertical list scrolling does not dismiss rows.
internal const val SwipeToDismissThresholdFraction = 0.5f
internal val LocalPodcastMiniPlayerInset = compositionLocalOf { 0.dp }

@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@SuppressLint("UnsafeOptInUsageError")
fun PodcastHomeScreen(
    modifier: Modifier = Modifier,
    initialEpisodeId: Long? = null,
    initialNotificationAction: String? = null,
    forceGettingStarted: Boolean = false,
    onInitialContentReady: () -> Unit = {},
    viewModel: PodcastViewModel = hiltViewModel(),
    playbackViewModel: PodcastPlaybackViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val homeUiState = rememberPodcastHomeUiState(viewModel, playbackViewModel)
    val podcasts = homeUiState.podcasts
    val allPodcasts = homeUiState.allPodcasts
    val latestEpisodePublishedAt = homeUiState.latestEpisodePublishedAt
    val inbox = viewModel.inboxPager.collectAsLazyPagingItems()
    val inboxSnapshot = remember(inbox.itemSnapshotList.items) { inbox.itemSnapshotList.items }
    val queueEntries = homeUiState.queueEntries
    val queueEpisodes = homeUiState.queueEpisodes
    val downloadAssets = homeUiState.downloadAssets
    val downloadedEpisodes = homeUiState.downloadedEpisodes
    val previewEpisodeEntities = homeUiState.previewEpisodeEntities
    val hasActiveDownloads = remember(downloadAssets) {
        downloadAssets.any { it.status in com.shapeshed.booth.data.PodcastDownloadManager.ACTIVE_STATUSES }
    }
    val state = homeUiState.homeState
    val localDiscoveryCategory = state.categoryDiscovery?.title?.let { title ->
        localTagDiscoveryCategory(title, podcasts)
    }
    val globalSearchQuery by viewModel.globalSearchQuery.collectAsStateWithLifecycle()
    val globalSearchEpisodes by viewModel.globalSearchEpisodes.collectAsStateWithLifecycle()
    val globalSearchState by viewModel.globalSearchState.collectAsStateWithLifecycle()
    val subscriptionProgress by PodcastSubscriptionProgressStore.progress.collectAsStateWithLifecycle()
    val refreshing = homeUiState.refreshing
    val subscriptionsViewMode = homeUiState.subscriptionsViewMode
    val playback = homeUiState.playback
    val settingsState = homeUiState.settings
    val globalPlaybackSpeed by viewModel.podcastPlaybackSpeed.collectAsStateWithLifecycle()
    val selectedSearchProvider = viewModel.searchProviders.firstOrNull {
        it.id == settingsState.searchProviderId
    }
    val podcastDownloadVideos = settingsState.downloadVideos
    val podcastAutoRefreshEnabled = settingsState.autoRefreshEnabled
    val podcastRefreshInterval = settingsState.refreshInterval
    val podcastRefreshNetwork = settingsState.refreshNetwork
    val podcastNotificationsEnabled = settingsState.notificationsEnabled
    val podcastAutoDownloadEnabled = settingsState.autoDownloadEnabled
    val podcastAutoQueueEnabled = settingsState.autoQueueEnabled
    val podcastDownloadNetwork = settingsState.downloadNetwork
    val savedPodcastTab = settingsState.savedPodcastTab
    val downloadProgress = homeUiState.downloadProgress
    val routeState = rememberPodcastHomeRouteState()
    // Each primary destination owns its own stack so switching tabs preserves nested routes and
    // destination state. NavDisplay receives only the currently selected stack.
    val homeBackStack = rememberNavBackStack(PodcastTab.HOME.toNavigationKey())
    val upNextBackStack = rememberNavBackStack(PodcastTab.UP_NEXT.toNavigationKey())
    val subscriptionsBackStack = rememberNavBackStack(PodcastTab.SUBSCRIPTIONS.toNavigationKey())
    val topLevelBackStack = when (routeState.selectedTab.value) {
        PodcastTab.HOME -> homeBackStack
        PodcastTab.UP_NEXT -> upNextBackStack
        PodcastTab.SUBSCRIPTIONS -> subscriptionsBackStack
    }
    val currentDestination = topLevelBackStack.lastOrNull()
    LaunchedEffect(currentDestination) {
        when (val destination = currentDestination) {
            is PodcastNavigationKey.DiscoveryCategory -> {
                if (destination.providerId == "local") {
                    viewModel.loadLocalTag(destination.title)
                } else {
                    val category = PodcastDiscoveryCategories.firstOrNull {
                        it.id.equals(destination.categoryId, ignoreCase = true) ||
                            it.appleGenreId.equals(destination.categoryId, ignoreCase = true) ||
                            it.title.equals(destination.title, ignoreCase = true)
                    } ?: PodcastDiscoveryCategory(
                        id = destination.categoryId,
                        title = destination.title,
                        appleGenreId = destination.categoryId,
                    )
                    viewModel.loadCategory(category)
                }
            }
            else -> Unit
        }
    }
    val showGlobalSearch = currentDestination == PodcastNavigationKey.GlobalSearch
    val showPodcastAppSettings = currentDestination == PodcastNavigationKey.Settings ||
        currentDestination is PodcastNavigationKey.PodcastManagement
    val showDiscovery = currentDestination is PodcastNavigationKey.Discovery ||
        currentDestination is PodcastNavigationKey.DiscoveryPodcast ||
        currentDestination is PodcastNavigationKey.DiscoveryCategory ||
        currentDestination is PodcastNavigationKey.DiscoveryEpisode
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val composeView = LocalView.current
    val inputMethodManager = remember(composeView) {
        composeView.context.getSystemService(InputMethodManager::class.java)
    }
    var showSearch by routeState.showSearch
    var pendingGlobalSubscriptions by remember { mutableStateOf<List<PodcastSearchResult>>(emptyList()) }
    var retainedPodcastAfterUnsubscribe by remember { mutableStateOf<PodcastEntity?>(null) }
    val globalSearchTextFieldState = rememberTextFieldState()
    val globalSearchBarState = androidx.compose.material3.rememberContainedSearchBarState()
    val globalSearchScrollBehavior = key(currentDestination) {
        SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    }
    val globalSearchFocusRequester = remember { FocusRequester() }
    val globalSearchExpanded by remember {
        derivedStateOf { globalSearchBarState.currentValue == SearchBarValue.Expanded }
    }
    val globalSearchText by remember {
        derivedStateOf { globalSearchTextFieldState.text.toString() }
    }
    var searchEpisodeSort by remember { mutableStateOf(SearchEpisodeSort.NEWEST) }
    var searchEpisodePlayedFilter by remember { mutableStateOf(SearchEpisodePlayedFilter.ALL) }
    var searchEpisodeTag by remember { mutableStateOf<String?>(null) }
    var selectedPodcastId by routeState.selectedPodcastId
    var selectedEpisodeId by routeState.selectedEpisodeId
    var podcastDetailPageId by routeState.podcastDetailPageId
    var selectedSearchEpisode by remember { mutableStateOf<EpisodeEntity?>(null) }
    var selectedSearchResult by remember { mutableStateOf<PodcastEpisodeSearchResult?>(null) }
    var episodeOrigin by routeState.episodeOrigin
    var showNowPlaying by routeState.showNowPlaying
    var podcastSortOrder by routeState.podcastSortOrder
    var showPodcastDescription by routeState.showPodcastDescription
    var showUnsubscribeConfirmation by routeState.showUnsubscribeConfirmation
    var podcastMenuExpanded by routeState.podcastMenuExpanded
    var selectedTab by routeState.selectedTab
    var showDiscoverySearch by routeState.showDiscoverySearch
    var showDiscoveryPodcastDescription by routeState.showDiscoveryPodcastDescription
    var rootMenuExpanded by routeState.rootMenuExpanded
    val showPodcastSettings = topLevelBackStack.lastOrNull() is PodcastNavigationKey.PodcastSettings
    val showDownloads = topLevelBackStack.lastOrNull() == PodcastNavigationKey.Downloads
    val showAllEpisodes = topLevelBackStack.lastOrNull() == PodcastNavigationKey.AllEpisodes
    var allEpisodeTags by routeState.allEpisodeTags
    var inboxSelectionMenuExpanded by routeState.inboxSelectionMenuExpanded
    var selectedInboxIds by routeState.selectedInboxIds
    var queueFilter by routeState.queueFilter
    var pendingEpisodeAction by routeState.pendingEpisodeAction
    var queueReorderMode by routeState.queueReorderMode
    var directFeedUrl by routeState.directFeedUrl
    var podcastDetailFromSubscriptions by routeState.podcastDetailFromSubscriptions
    var gettingStartedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedSearchProvider?.id) {
        gettingStartedCategoryId = null
    }
    LaunchedEffect(forceGettingStarted) {
        if (forceGettingStarted) {
            selectedTab = PodcastTab.SUBSCRIPTIONS
            showSearch = false
            topLevelBackStack.clear()
            topLevelBackStack.add(PodcastNavigationKey.Subscriptions)
        }
    }
    LaunchedEffect(Unit) {
        // Keep the platform splash visible while the first empty-library discovery shelf loads.
        // Existing libraries release immediately; the one-second limit matches Android's
        // recommended maximum splash animation duration and avoids blocking startup on network.
        withTimeoutOrNull(1_000L) {
            combine(viewModel.podcasts, viewModel.state) { subscribedPodcasts, homeState ->
                subscribedPodcasts.isNotEmpty() || homeState.discoveryLoaded || homeState.error != null
            }.first { it }
        }
        onInitialContentReady()
    }
    LaunchedEffect(globalSearchTextFieldState) {
        snapshotFlow { globalSearchTextFieldState.text.toString() }
            .distinctUntilChanged()
            .collect(viewModel::setGlobalSearchQuery)
    }
    LaunchedEffect(globalSearchQuery) {
        if (globalSearchTextFieldState.text.toString() != globalSearchQuery) {
            globalSearchTextFieldState.setTextAndPlaceCursorAtEnd(globalSearchQuery)
        }
    }
    LaunchedEffect(podcasts) {
        val subscribedFeedUrls = podcasts.map { it.feedUrl }.toSet()
        pendingGlobalSubscriptions = pendingGlobalSubscriptions.filterNot {
            it.podcast.feedUrl in subscribedFeedUrls
        }
    }
    LaunchedEffect(globalSearchExpanded) {
        if (globalSearchExpanded) {
            viewModel.loadDiscovery()
            withFrameNanos { }
            withFrameNanos { }
            globalSearchFocusRequester.requestFocus()
            keyboardController?.show()
            composeView.post {
                if (globalSearchExpanded && composeView.hasWindowFocus()) {
                    inputMethodManager?.showSoftInput(
                        composeView,
                        0,
                    )
                }
            }
        } else {
            keyboardController?.hide()
        }
    }
    LaunchedEffect(podcasts.isEmpty(), showSearch) {
        if (podcasts.isEmpty() && !showSearch) {
            viewModel.loadDiscovery()
        }
    }
    val derived = rememberPodcastHomeDerivedState(
        viewModel = viewModel,
        podcasts = podcasts,
        catalogIndex = homeUiState.podcastCatalogIndex,
        queueEntries = queueEntries,
        queueEpisodes = queueEpisodes,
        downloadedEpisodes = downloadedEpisodes,
        selectedPodcastId = if (podcastDetailFromSubscriptions) {
            podcastDetailPageId ?: selectedPodcastId
        } else {
            selectedPodcastId
        },
        selectedEpisodeId = selectedEpisodeId,
        homeState = state,
        playback = playback,
    )
    val allPodcastsById = remember(allPodcasts) { allPodcasts.associateBy(PodcastEntity::id) }
    val selectedPodcast = derived.selectedPodcast
        ?: allPodcastsById[selectedPodcastId]
        ?: retainedPodcastAfterUnsubscribe?.takeIf { it.id == selectedPodcastId }
    val podcastsById = derived.podcastsById
    val podcastTitlesById = remember(allPodcastsById) {
        allPodcastsById.mapValues { (_, podcast) -> podcast.title }
    }
    val availablePodcastTags = derived.availablePodcastTags
    val queueEpisodeIds = derived.queueEpisodeIds
    val subscribedFeedUrls = derived.subscribedFeedUrls
    // A search result is already a complete episode projection. Use it while the selected
    // podcast's Room flow is catching up so navigation never lands on an empty detail pane.
    val selectedEpisode = derived.selectedEpisode
        ?: selectedSearchEpisode?.takeIf { it.id == selectedEpisodeId }
        ?: globalSearchEpisodes.firstOrNull { it.episode.id == selectedEpisodeId }?.episode
    val visibleSelectedEpisodes = derived.visibleSelectedEpisodes
    val discoveryDescriptionBlocks = derived.discoveryDescriptionBlocks
    val playingPodcastTitle = derived.playingPodcastTitle
    val miniPlayerVisible = playback.episode != null
    var miniPlayerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val miniPlayerBottomInset = if (miniPlayerVisible && !showPodcastAppSettings && miniPlayerHeightPx > 0) {
        with(density) { miniPlayerHeightPx.toDp() }
    } else {
        0.dp
    }
    val platformActions = rememberPodcastHomePlatformActions(context, viewModel) {
        topLevelBackStack.clear()
        topLevelBackStack.add(PodcastNavigationKey.Subscriptions)
    }
    val scope = rememberCoroutineScope()
    fun openSettings() {
        topLevelBackStack.add(PodcastNavigationKey.Settings)
    }

    fun openPodcastManagement(category: PodcastManagementCategory) {
        topLevelBackStack.add(PodcastNavigationKey.PodcastManagement(category))
    }
    fun openDiscoveryPodcast(result: PodcastSearchResult, fromSearch: Boolean = false) {
        val fromGettingStarted = !fromSearch && selectedTab == PodcastTab.SUBSCRIPTIONS &&
            (podcasts.isEmpty() || forceGettingStarted) && !showSearch
        if (fromSearch) {
            val root = topLevelBackStack.firstOrNull() ?: selectedTab.toNavigationKey()
            topLevelBackStack.clear()
            topLevelBackStack.add(root)
            topLevelBackStack.add(PodcastNavigationKey.GlobalSearch)
        } else if (fromGettingStarted) {
            topLevelBackStack.clear()
            topLevelBackStack.add(PodcastNavigationKey.Subscriptions)
        } else if (topLevelBackStack.lastOrNull() != PodcastNavigationKey.Discovery) {
            topLevelBackStack.add(PodcastNavigationKey.Discovery)
        }
        topLevelBackStack.add(PodcastNavigationKey.DiscoveryPodcast(result.podcast.feedUrl))
        viewModel.preview(result)
    }
    fun openDiscoveryCategory(
        category: PodcastDiscoveryCategory,
        providerId: String = settingsState.searchProviderId,
    ) {
        val routeCategoryId = if (providerId == APPLE_DIRECTORY_PROVIDER_ID) {
            category.appleGenreId
        } else {
            category.id
        }
        topLevelBackStack.add(
            PodcastNavigationKey.DiscoveryCategory(
                providerId = providerId,
                categoryId = routeCategoryId,
                title = category.title,
            ),
        )
    }
    fun openDiscoveryEpisode(episode: Episode) {
        topLevelBackStack.add(PodcastNavigationKey.DiscoveryEpisode(episode.id))
        viewModel.openPreviewEpisode(episode)
    }
    fun openGlobalSearch() {
        if (topLevelBackStack.lastOrNull() != PodcastNavigationKey.GlobalSearch) {
            topLevelBackStack.add(PodcastNavigationKey.GlobalSearch)
        }
        scope.launch { globalSearchBarState.animateToExpanded() }
    }
    fun closeGlobalSearch() {
        if (topLevelBackStack.lastOrNull() == PodcastNavigationKey.GlobalSearch) {
            topLevelBackStack.removeLastOrNull()
        }
        viewModel.clearGlobalSearch()
        scope.launch { globalSearchBarState.animateToCollapsed() }
    }
    fun selectTabFromHome(tab: PodcastTab) {
        closeGlobalSearch()
        globalSearchTextFieldState.setTextAndPlaceCursorAtEnd("")
        selectPodcastTab(routeState, viewModel, tab)
    }
    fun collapseGlobalSearch() {
        scope.launch { globalSearchBarState.animateToCollapsed() }
    }
    fun openSpecialRoot(destination: PodcastNavigationKey) {
        selectedPodcastId = null
        selectedEpisodeId = null
        topLevelBackStack.clear()
        topLevelBackStack.add(selectedTab.toNavigationKey())
        topLevelBackStack.add(destination)
    }
    fun openPodcastSettings() {
        selectedPodcast?.let { podcast ->
            if (topLevelBackStack.lastOrNull() !is PodcastNavigationKey.PodcastSettings) {
                topLevelBackStack.add(PodcastNavigationKey.PodcastSettings(podcast.id))
            }
        }
    }
    fun popTopLevelRoute() {
        // NavDisplay requires one root destination to remain in the back stack. Keep
        // toolbar, gesture, and system back handling consistent at the root.
        if (topLevelBackStack.size <= 1) return
        val popped = topLevelBackStack.removeLastOrNull() ?: return
        when (popped) {
            is PodcastNavigationKey.DiscoveryEpisode -> viewModel.closePreviewEpisode()
            is PodcastNavigationKey.DiscoveryCategory -> viewModel.closeCategory()
            is PodcastNavigationKey.DiscoveryPodcast -> viewModel.closePreview()
            PodcastNavigationKey.GlobalSearch -> {
                viewModel.clearGlobalSearch()
                scope.launch { globalSearchBarState.animateToCollapsed() }
            }
            else -> Unit
        }
        if (topLevelBackStack.lastOrNull() == PodcastNavigationKey.GlobalSearch) {
            scope.launch { globalSearchBarState.animateToExpanded() }
        }
        when (val destination = topLevelBackStack.lastOrNull()) {
            is PodcastNavigationKey.EpisodeDetail -> {
                selectedEpisodeId = destination.episodeId
            }
            is PodcastNavigationKey.PodcastDetail -> {
                selectedEpisodeId = null
                selectedPodcastId = destination.podcastId
                podcastDetailPageId = destination.podcastId
            }
            else -> {
                selectedEpisodeId = null
                selectedPodcastId = null
                podcastDetailPageId = null
                podcastDetailFromSubscriptions = false
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val podcastFallback = stringResource(R.string.podcast_fallback)
    val couldNotAddPodcastFormat = stringResource(R.string.could_not_add_podcast)
    val removeUpNextFailed = stringResource(R.string.error_remove_up_next)
    val addUpNextFailed = stringResource(R.string.error_add_up_next)
    var notifiedSubscriptionFailures by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(state.error) {
        val result = state.error as? PodcastUiError.ImportCompleted ?: return@LaunchedEffect
        val message = if (result.imported == result.total) {
            context.resources.getQuantityString(
                R.plurals.imported_podcasts,
                result.imported,
                result.imported,
            )
        } else {
            context.resources.getQuantityString(
                R.plurals.imported_podcasts_partial,
                result.imported,
                result.imported,
                result.total,
            )
        }
        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
    }
    fun enqueueFeedSubscription(
        feedUrl: String,
        title: String? = null,
        fallbackDescriptionHtml: String? = null,
        appleCategories: List<String> = emptyList(),
        appleCategoryIds: Map<String, String> = emptyMap(),
        categoryProviderId: String = "apple",
    ) {
        if (feedUrl.isBlank()) return
        notifiedSubscriptionFailures = notifiedSubscriptionFailures - feedUrl
        viewModel.subscribeInBackground(
            context = context,
            feedUrl = feedUrl,
            title = title,
            fallbackDescriptionHtml = fallbackDescriptionHtml,
            appleCategories = appleCategories,
            appleCategoryIds = appleCategoryIds,
            categoryProviderId = categoryProviderId,
        )
    }
    fun enqueueSubscription(result: PodcastSearchResult, keepPendingResult: Boolean = false) {
        if (keepPendingResult) {
            pendingGlobalSubscriptions = (pendingGlobalSubscriptions + result)
                .distinctBy { it.podcast.feedUrl }
        }
        notifiedSubscriptionFailures = notifiedSubscriptionFailures - result.podcast.feedUrl
        enqueueFeedSubscription(
            feedUrl = result.podcast.feedUrl,
            title = result.podcast.title,
            fallbackDescriptionHtml = result.podcast.descriptionHtml,
            appleCategories = result.podcast.categories,
            appleCategoryIds = result.podcast.categoryIds,
            categoryProviderId = result.providerId,
        )
    }
    LaunchedEffect(subscriptionProgress) {
        subscriptionProgress.forEach { (feedUrl, result) ->
            if (result.stage != PodcastSubscriptionStage.FAILED ||
                feedUrl in notifiedSubscriptionFailures) {
                return@forEach
            }
            notifiedSubscriptionFailures = notifiedSubscriptionFailures + feedUrl
            val title = result.title
                ?: pendingGlobalSubscriptions
                .firstOrNull { it.podcast.feedUrl == feedUrl }
                ?.podcast
                ?.title
                ?: podcastFallback
            val message = String.format(Locale.getDefault(), couldNotAddPodcastFormat, title)
            if (result.stage == PodcastSubscriptionStage.FAILED) {
                pendingGlobalSubscriptions = pendingGlobalSubscriptions.filterNot {
                    it.podcast.feedUrl == feedUrl
                }
            }
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    val undoActions = remember(viewModel, snackbarHostState) {
        PodcastHomeUndoActions(
            context = context,
            scope = scope,
            snackbarHostState = snackbarHostState,
            viewModel = viewModel,
        )
    }
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val adaptiveLayout = podcastAdaptiveLayout(windowSizeClass::isWidthAtLeastBreakpoint)
    val useNavigationRail = adaptiveLayout.useNavigationRail
    val useTwoPaneEpisodeLayout = adaptiveLayout.useTwoPaneEpisodeLayout
    val navigationRailStartPadding = if (useNavigationRail) {
        80.dp + with(LocalDensity.current) {
            WindowInsets.navigationBars.getLeft(this, LocalLayoutDirection.current).toDp()
        }
    } else {
        0.dp
    }
    val atRoot = selectedPodcast == null && selectedEpisode == null && !showDiscovery &&
        !showPodcastAppSettings && !showDownloads && !showAllEpisodes
    val inboxSelectionMode = selectedTab == PodcastTab.HOME && selectedInboxIds.isNotEmpty() && atRoot

    PodcastHomeEffects(
        context = context,
        routeState = routeState,
        viewModel = viewModel,
        initialEpisodeId = initialEpisodeId,
        initialNotificationAction = initialNotificationAction,
        playbackViewModel = playbackViewModel,
        autoRefreshEnabled = podcastAutoRefreshEnabled,
        refreshInterval = podcastRefreshInterval,
        refreshNetwork = podcastRefreshNetwork,
        savedPodcastTab = savedPodcastTab,
        showNowPlaying = showNowPlaying,
        playbackEpisodeId = playback.episode?.id,
        showGlobalSearch = showGlobalSearch,
        hasActiveDownloads = hasActiveDownloads,
        selectedEpisode = selectedEpisode,
        onOpenInitialEpisode = { episode ->
            episodeOrigin = EpisodeOrigin.PODCAST
            selectedPodcastId = episode.podcastId
            selectedEpisodeId = episode.id
            topLevelBackStack.clear()
            topLevelBackStack.add(selectedTab.toNavigationKey())
            topLevelBackStack.add(
                PodcastNavigationKey.EpisodeDetail(
                    episodeId = episode.id,
                    origin = EpisodeNavigationOrigin.Podcast,
                ),
            )
        },
        onSelectSavedTab = { tab ->
            selectedTab = tab
            topLevelBackStack.clear()
            topLevelBackStack.add(tab.toNavigationKey())
        },
    )
    PodcastHomeBackHandlers(
        routeState = routeState,
        playbackViewModel = playbackViewModel,
        inboxSelectionMode = inboxSelectionMode,
        showGlobalSearch = showGlobalSearch,
        onCloseGlobalSearch = ::closeGlobalSearch,
    )
    fun dismissNowPlaying() {
        playbackViewModel.switchToAudioForBackground()
        showNowPlaying = false
    }
    val globalSearchResults: @Composable () -> Unit = {
        PodcastHomeGlobalSearchDestination(
            selectedTab = selectedTab,
            query = globalSearchQuery,
            episodes = globalSearchEpisodes,
            subscriptions = podcasts,
            remotePodcasts = globalSearchState.remotePodcasts,
            popularPodcasts = if (selectedSearchProvider?.supportsPopularPodcasts == true) {
                state.discovery.firstOrNull()?.results.orEmpty()
            } else {
                emptyList()
            },
            pendingSubscriptions = pendingGlobalSubscriptions,
            isLoadingRemote = globalSearchState.isLoadingRemote,
            error = globalSearchState.error,
            onOpenEpisode = { episode ->
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                scope.launch { globalSearchBarState.animateToCollapsed() }
                collapseGlobalSearch()
                selectedSearchEpisode = episode
                selectedSearchResult = globalSearchEpisodes.firstOrNull { it.episode.id == episode.id }
                episodeOrigin = EpisodeOrigin.SEARCH
                selectedPodcastId = episode.podcastId
                selectedEpisodeId = episode.id
                topLevelBackStack.clear()
                topLevelBackStack.add(selectedTab.toNavigationKey())
                topLevelBackStack.add(PodcastNavigationKey.GlobalSearch)
                topLevelBackStack.add(
                    PodcastNavigationKey.EpisodeDetail(
                        episodeId = episode.id,
                        origin = EpisodeNavigationOrigin.Search,
                    ),
                )
            },
            onOpenPodcast = { podcastId ->
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                scope.launch { globalSearchBarState.animateToCollapsed() }
                collapseGlobalSearch()
                podcastDetailFromSubscriptions = false
                podcastDetailPageId = null
                selectedPodcastId = podcastId
                topLevelBackStack.clear()
                topLevelBackStack.add(selectedTab.toNavigationKey())
                topLevelBackStack.add(PodcastNavigationKey.GlobalSearch)
                topLevelBackStack.add(PodcastNavigationKey.PodcastDetail(podcastId))
            },
            onOpenRemotePodcast = { result ->
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                collapseGlobalSearch()
                openDiscoveryPodcast(result, fromSearch = true)
            },
            onSubscribe = { result ->
                enqueueSubscription(result, keepPendingResult = true)
            },
            availableTags = (availablePodcastTags + derived.availableAppleCategories).distinct().sorted(),
            selectedTag = searchEpisodeTag,
            onSelectedTagChange = { searchEpisodeTag = it },
            sort = searchEpisodeSort,
            onSortChange = { searchEpisodeSort = it },
            playedFilter = searchEpisodePlayedFilter,
            onPlayedFilterChange = { searchEpisodePlayedFilter = it },
            modifier = Modifier.fillMaxSize(),
        )
    }
    @Composable
    fun SettingsContent() {
        PodcastHomeSettingsDestination(
            homeUiState = homeUiState,
            globalPlaybackSpeed = globalPlaybackSpeed,
            viewModel = viewModel,
            playbackViewModel = playbackViewModel,
            platformActions = platformActions,
            onManagePodcasts = ::openPodcastManagement,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    fun DiscoveryContent() {
        PodcastHomeDiscoveryDestination(
            homeUiState = homeUiState,
            viewModel = viewModel,
            playbackViewModel = playbackViewModel,
            context = context,
            queueEpisodeIds = queueEpisodeIds,
            subscribedFeedUrls = subscribedFeedUrls,
            onSubscribe = { result -> enqueueSubscription(result) },
            onUnsubscribe = {
                podcasts.firstOrNull { it.feedUrl == state.previewResult?.podcast?.feedUrl }
                    ?.let(viewModel::remove)
            },
            onShowDescription = { showDiscoveryPodcastDescription = true },
            onShowNowPlayingChange = { showNowPlaying = it },
            onEpisodeAction = { pendingEpisodeAction = it },
            onQueueError = {
                scope.launch { snackbarHostState.showSnackbar(addUpNextFailed) }
            },
            onOpenPodcast = ::popTopLevelRoute,
            onCategory = ::openDiscoveryCategory,
            onEpisode = ::openDiscoveryEpisode,
            twoPane = useTwoPaneEpisodeLayout,
            modifier = Modifier.fillMaxSize(),
        )
    }

    val useNavigationDetails = !globalSearchExpanded
    val toolbarMotionScheme = MaterialTheme.motionScheme
    Box(modifier = modifier.fillMaxSize()) {
        PodcastHomeAppContent(
            modifier = if (atRoot) {
                Modifier.nestedScroll(globalSearchScrollBehavior.nestedScrollConnection)
            } else {
                Modifier
            },
            navigationRailStartPadding = navigationRailStartPadding,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.zIndex(10f),
            )
        },
        topBar = {
            AnimatedContent(
                targetState = inboxSelectionMode,
                transitionSpec = {
                    fadeIn(
                        animationSpec = toolbarMotionScheme.defaultEffectsSpec(),
                    ) togetherWith fadeOut(
                        animationSpec = toolbarMotionScheme.defaultEffectsSpec(),
                    )
                },
                label = "inbox selection toolbar",
            ) { selectionMode ->
            Column {
            if (atRoot && useNavigationRail && !selectionMode) {
                TopAppBar(
                    title = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier.fillMaxWidth(),
                            textFieldState = globalSearchTextFieldState,
                            searchBarState = globalSearchBarState,
                            onSearch = { scope.launch { globalSearchBarState.animateToCollapsed() } },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search,
                                showKeyboardOnFocus = true,
                            ),
                            placeholder = {
                                Text(
                                    if (selectedTab == PodcastTab.SUBSCRIPTIONS) {
                                        stringResource(R.string.search_podcasts)
                                    } else {
                                        stringResource(R.string.search_episodes)
                                    },
                                )
                            },
                            leadingIcon = {
                                if (globalSearchExpanded) {
                                    IconButton(onClick = {
                                        scope.launch { globalSearchBarState.animateToCollapsed() }
                                    }) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                                    }
                                } else {
                                    Icon(Icons.Rounded.Search, contentDescription = null)
                                }
                            },
                            trailingIcon = {
                                if (globalSearchText.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearGlobalSearch() }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                                    }
                                }
                            },
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    actions = {
                        PodcastHomeRootActions(
                            selectedTab = selectedTab,
                            queueReorderMode = queueReorderMode,
                            menuExpanded = rootMenuExpanded,
                            viewModel = viewModel,
                            showSearchAction = false,
                            onQueueReorderDone = { queueReorderMode = false },
                            onOpenDiscoverySearch = ::openGlobalSearch,
                            onOpenAddPodcast = { routeState.showAddPodcast.value = true },
                            onMenuExpandedChange = { rootMenuExpanded = it },
                            onOpenDownloads = { openSpecialRoot(PodcastNavigationKey.Downloads) },
                            onOpenAllEpisodes = { openSpecialRoot(PodcastNavigationKey.AllEpisodes) },
                            onOpenSettings = ::openSettings,
                        )
                    },
                )
            } else if (atRoot && !selectionMode) {
                AppBarWithSearch(
                    state = globalSearchBarState,
                    inputField = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier.focusRequester(globalSearchFocusRequester),
                            textFieldState = globalSearchTextFieldState,
                            searchBarState = globalSearchBarState,
                            onSearch = { scope.launch { globalSearchBarState.animateToCollapsed() } },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search,
                                showKeyboardOnFocus = true,
                            ),
                            placeholder = {
                                Text(
                                    if (selectedTab == PodcastTab.SUBSCRIPTIONS) {
                                        stringResource(R.string.search_podcasts)
                                    } else {
                                        stringResource(R.string.search_episodes)
                                    },
                                )
                            },
                            leadingIcon = {
                                if (globalSearchExpanded) {
                                    IconButton(onClick = {
                                        scope.launch { globalSearchBarState.animateToCollapsed() }
                                    }) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                                    }
                                } else {
                                    Icon(Icons.Rounded.Search, contentDescription = null)
                                }
                            },
                            trailingIcon = {
                                if (globalSearchText.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearGlobalSearch() }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                                    }
                                }
                            },
                        )
                    },
                    colors = SearchBarDefaults.appBarWithSearchColors(
                        searchBarColors = SearchBarDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        scrolledSearchBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        appBarContainerColor = MaterialTheme.colorScheme.surface,
                        scrolledAppBarContainerColor = MaterialTheme.colorScheme.surface,
                        appBarNavigationIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        appBarActionIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    scrollBehavior = globalSearchScrollBehavior,
                    actions = {
                        PodcastHomeRootActions(
                            selectedTab = selectedTab,
                            queueReorderMode = queueReorderMode,
                            menuExpanded = rootMenuExpanded,
                            viewModel = viewModel,
                            showSearchAction = false,
                            onQueueReorderDone = { queueReorderMode = false },
                            onOpenDiscoverySearch = ::openGlobalSearch,
                            onOpenAddPodcast = { routeState.showAddPodcast.value = true },
                            onMenuExpandedChange = { rootMenuExpanded = it },
                            onOpenDownloads = { openSpecialRoot(PodcastNavigationKey.Downloads) },
                            onOpenAllEpisodes = { openSpecialRoot(PodcastNavigationKey.AllEpisodes) },
                            onOpenSettings = ::openSettings,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else TopAppBar(
                title = {
                    PodcastHomeTopBarTitle(
                        inboxSelectionMode = selectionMode,
                        selectedInboxCount = selectedInboxIds.size,
                        showPodcastAppSettings = showPodcastAppSettings,
                        podcastManagementCategory = (currentDestination as? PodcastNavigationKey.PodcastManagement)?.category,
                        showDownloads = showDownloads,
                        showAllEpisodes = showAllEpisodes,
                        showPodcastSettings = showPodcastSettings,
                        podcastSettingsTitle = selectedPodcast?.title,
                        showDiscovery = showDiscovery,
                        discoveryTitle = state.categoryDiscovery?.title,
                        hasSelectedPodcast = selectedPodcast != null,
                        hasSelectedEpisode = selectedEpisode != null,
                        selectedTab = selectedTab,
                    )
                },
                colors = if (selectionMode) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
                navigationIcon = {
                    PodcastHomeTopBarNavigationIcon(
                        inboxSelectionMode = selectionMode,
                        showPodcastAppSettings = showPodcastAppSettings,
                        showDownloads = showDownloads,
                        showAllEpisodes = showAllEpisodes,
                        showDiscovery = showDiscovery,
                        hasSelectedPodcast = selectedPodcast != null,
                        hasSelectedEpisode = selectedEpisode != null,
                        onCloseSelection = { selectedInboxIds = emptySet() },
                        onBack = {
                            when {
                                showPodcastAppSettings || showDiscovery -> popTopLevelRoute()
                                showDownloads || showAllEpisodes || showPodcastSettings ->
                                    popTopLevelRoute()
                                selectedEpisode != null -> {
                                    popTopLevelRoute()
                                }
                                else -> {
                                    popTopLevelRoute()
                                }
                            }
                        },
                    )
                },
                actions = {
                    if (selectionMode) {
                        PodcastHomeInboxSelectionActions(
                            inbox = inboxSnapshot,
                            selectedIds = selectedInboxIds,
                            menuExpanded = inboxSelectionMenuExpanded,
                            viewModel = viewModel,
                            undoActions = undoActions,
                            podcastsById = podcastsById,
                            downloadProgress = downloadProgress,
                            queueEpisodeIds = queueEpisodeIds,
                            scope = scope,
                            snackbarHostState = snackbarHostState,
                            onMenuExpandedChange = { inboxSelectionMenuExpanded = it },
                            onClearSelection = { selectedInboxIds = emptySet() },
                            onPendingAction = { pendingEpisodeAction = it },
                            context = context,
                        )
                    } else if (atRoot) {
                        PodcastHomeRootActions(
                            selectedTab = selectedTab,
                            queueReorderMode = queueReorderMode,
                            menuExpanded = rootMenuExpanded,
                            viewModel = viewModel,
                            onQueueReorderDone = { queueReorderMode = false },
                            onOpenDiscoverySearch = ::openGlobalSearch,
                            onOpenAddPodcast = { routeState.showAddPodcast.value = true },
                            onMenuExpandedChange = { rootMenuExpanded = it },
                            onOpenDownloads = { openSpecialRoot(PodcastNavigationKey.Downloads) },
                            onOpenAllEpisodes = { openSpecialRoot(PodcastNavigationKey.AllEpisodes) },
                        onOpenSettings = ::openSettings,
                        )
                    } else {
                        PodcastHomeContextActions(
                            showPodcastAppSettings = showPodcastAppSettings,
                            showPodcastSettings = showPodcastSettings,
                            showDiscovery = showDiscovery,
                            hasCategoryDiscovery = state.categoryDiscovery != null,
                            isLocalCategoryDiscovery = state.categoryDiscovery != null &&
                                state.categoryDiscoveryCategory == null &&
                                state.previewResult == null &&
                                localDiscoveryCategory != null,
                            hasPreviewEpisode = state.previewEpisode != null,
                            hasPreviewResult = state.previewResult != null,
                            selectedEpisode = selectedEpisode,
                            selectedPodcast = selectedPodcast,
                            isPodcastSubscribed = selectedPodcast?.id in podcastsById ||
                                selectedPodcast?.feedUrl
                                    ?.let { feedUrl ->
                                        subscriptionProgress[feedUrl]?.stage == PodcastSubscriptionStage.ADDED
                                    } == true,
                            isPodcastSubscriptionLoading = selectedPodcast?.feedUrl
                                ?.let { feedUrl ->
                                    subscriptionProgress[feedUrl]?.stage?.isInProgress == true
                                } == true,
                            isDiscoverySubscribed = state.previewResult?.podcast?.feedUrl
                                ?.let(::canonicalFeedUrl)
                                ?.let { canonicalUrl -> subscribedFeedUrls.any { canonicalFeedUrl(it) == canonicalUrl } }
                                == true || state.previewResult?.podcast?.feedUrl
                                ?.let { feedUrl ->
                                    subscriptionProgress[feedUrl]?.stage == PodcastSubscriptionStage.ADDED
                                } == true,
                            isDiscoverySubscriptionLoading = state.previewResult?.podcast?.feedUrl
                                ?.let { feedUrl ->
                                    subscriptionProgress[feedUrl]?.stage?.isInProgress == true
                                } == true,
                            podcastMenuExpanded = podcastMenuExpanded,
                            context = context,
                            onShowDiscoverySearch = { showDiscoverySearch = true },
                            onDiscoverCategory = {
                                localDiscoveryCategory?.let {
                                    openDiscoveryCategory(it.category, it.providerId)
                                }
                            },
                            onPodcastMenuExpandedChange = { podcastMenuExpanded = it },
                            onPodcastSettings = ::openPodcastSettings,
                            onUnsubscribe = { showUnsubscribeConfirmation = true },
                            onSubscribe = {
                                selectedPodcast?.let { podcast ->
                                    enqueueSubscription(
                                        PodcastSearchResult(
                                            providerId = "local",
                                            podcast = com.shapeshed.booth.data.Podcast(
                                                id = podcast.id,
                                                title = podcast.title,
                                                author = podcast.author,
                                                feedUrl = podcast.feedUrl,
                                                siteUrl = podcast.siteUrl,
                                        descriptionHtml = podcast.descriptionHtml,
                                        artworkUrl = podcast.artworkUrl,
                                        explicit = podcast.explicit,
                                        categories = podcast.categories.map { it.name },
                                        categoryIds = podcast.categories.mapNotNull { category ->
                                            category.externalId?.let { category.name to it }
                                        }.toMap(),
                                            ),
                                        ),
                                    )
                                }
                            },
                            onDiscoverySubscription = {
                                state.previewResult?.let { result ->
                                    val resultFeedUrl = canonicalFeedUrl(result.podcast.feedUrl)
                                    if (subscribedFeedUrls.any { canonicalFeedUrl(it) == resultFeedUrl }) {
                                        podcasts.firstOrNull { canonicalFeedUrl(it.feedUrl) == resultFeedUrl }
                                            ?.let(viewModel::remove)
                                    } else {
                                        enqueueSubscription(result)
                                    }
                                }
                            },
                            onDiscoverySettings = {
                                state.previewResult?.let { result ->
                                    podcasts.firstOrNull {
                                        canonicalFeedUrl(it.feedUrl) == canonicalFeedUrl(result.podcast.feedUrl)
                                    }?.let { podcast ->
                                        selectedPodcastId = podcast.id
                                        if (topLevelBackStack.lastOrNull() !is PodcastNavigationKey.PodcastSettings) {
                                            topLevelBackStack.add(PodcastNavigationKey.PodcastSettings(podcast.id))
                                        }
                                    }
                                }
                            },
                            onDiscoveryShare = {
                                state.previewResult?.let { result ->
                                    shareLink(
                                        context,
                                        result.podcast.title,
                                        result.podcast.siteUrl ?: result.podcast.feedUrl,
                                    )
                                }
                            },
                            onDiscoveryOpenInBrowser = {
                                state.previewResult?.let { result ->
                                    openExternally(
                                        context,
                                        result.podcast.siteUrl ?: result.podcast.feedUrl,
                                    )
                                }
                            },
                        )
                    }
                },
            )
            if (refreshing) {
                val progress = homeUiState.refreshProgress
                if (progress.total > 0) {
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            }
            }
        },
        bottomBar = {
            PodcastHomeBottomNavigation(
                visible = !useNavigationRail && atRoot,
                selectedTab = selectedTab,
                inboxCount = inbox.itemCount,
                onTabSelected = ::selectTabFromHome,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
        PodcastHomeNavigationRail(
            visible = false,
            selectedTab = selectedTab,
            inboxCount = inbox.itemCount,
            onTabSelected = ::selectTabFromHome,
        )
        Box(
            Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalPodcastMiniPlayerInset provides miniPlayerBottomInset,
        ) {
        if (atRoot && globalSearchExpanded) {
            globalSearchResults()
        } else if (useNavigationDetails) {
            @Composable
            fun RootTabContent(tab: PodcastTab) {
                if (tab == PodcastTab.HOME) {
                    PodcastHomeInboxDestination(
                inbox = inbox,
                podcastsById = podcastsById,
                playback = playback,
                selectionMode = inboxSelectionMode,
                selectedEpisodeIds = selectedInboxIds,
                onSelectedEpisodeIdsChange = { selectedInboxIds = it },
                onOpen = { episode ->
                    episodeOrigin = EpisodeOrigin.INBOX
                    selectedPodcastId = episode.podcastId
                    viewModel.resolveMediaSizes(episode)
                    selectedEpisodeId = episode.id
                    topLevelBackStack.clear()
                    topLevelBackStack.add(PodcastNavigationKey.Inbox)
                    topLevelBackStack.add(
                        PodcastNavigationKey.EpisodeDetail(
                            episode.id,
                            EpisodeNavigationOrigin.Inbox,
                        ),
                    )
                },
                onAddToQueue = { undoActions.requestInboxAction(it, addToQueue = true) },
                onDismiss = { undoActions.requestInboxAction(it, addToQueue = false) },
                onActions = { episode ->
                    pendingEpisodeAction = createSubscribedEpisodeAction(
                        episode = episode,
                        podcastsById = podcastsById,
                        downloadProgress = downloadProgress,
                        queueEpisodeIds = queueEpisodeIds,
                    )
                },
                onPlay = { episode ->
                    if (playback.episode?.id == episode.id) playbackViewModel.togglePlayPause()
                    else playbackViewModel.play(episode, podcastsById[episode.podcastId]?.title.orEmpty())
                },
                onDownload = { viewModel.download(context, it.id) },
                onRefresh = viewModel::refreshSubscriptions,
                refreshing = refreshing,
                downloadProgress = downloadProgress,
                modifier = Modifier.fillMaxSize(),
                    )
                } else if (tab == PodcastTab.UP_NEXT) {
                    PodcastHomeUpNextDestination(
                        episodes = queueEpisodes,
                        podcastsById = allPodcastsById,
                        playback = playback,
                        downloadProgress = downloadProgress,
                        viewModel = viewModel,
                        context = context,
                        reorderMode = queueReorderMode,
                        filter = queueFilter,
                        onFilterChange = { queueFilter = it },
                        onOpen = { episode ->
                            episodeOrigin = EpisodeOrigin.UP_NEXT
                            selectedPodcastId = episode.podcastId
                            viewModel.resolveMediaSizes(episode)
                            selectedEpisodeId = episode.id
                            topLevelBackStack.clear()
                            topLevelBackStack.add(PodcastNavigationKey.UpNext)
                            topLevelBackStack.add(
                                PodcastNavigationKey.EpisodeDetail(
                                    episode.id,
                                    EpisodeNavigationOrigin.UpNext,
                                ),
                            )
                        },
                        onLongPress = { episode ->
                            pendingEpisodeAction = createSubscribedEpisodeAction(
                                episode = episode,
                                podcastsById = podcastsById,
                                downloadProgress = downloadProgress,
                                queueEpisodeIds = queueEpisodeIds,
                            )
                        },
                        onRemoveFailed = { scope.launch { snackbarHostState.showSnackbar(removeUpNextFailed) } },
                        onReorder = viewModel::reorderQueue,
                        onPlay = { episode ->
                            if (playback.episode?.id == episode.id) playbackViewModel.togglePlayPause()
                            else playbackViewModel.playQueue(
                                episodes = queueEpisodes,
                                selectedEpisode = episode,
                                podcastTitles = allPodcastsById.mapValues { (_, podcast) -> podcast.title },
                                useVideo = (episode.preferVideo || (!episode.videoPreferenceSet && episode.isVideoOnlySource())) &&
                                    !episode.videoUrl.isNullOrBlank(),
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    PodcastSubscriptionsContent(
                podcasts = if (forceGettingStarted && !showSearch) {
                    emptyList()
                } else {
                    podcasts
                },
                latestEpisodePublishedAt = latestEpisodePublishedAt,
                viewMode = subscriptionsViewMode,
                onViewModeChange = viewModel::setSubscriptionsViewMode,
                sortOrder = podcastSortOrder,
                onSortOrderChange = { podcastSortOrder = it },
                state = state,
                showSearch = showSearch,
                directFeedUrl = directFeedUrl,
                onDirectFeedUrlChange = { directFeedUrl = it },
                onSubscribeDirect = {
                    enqueueFeedSubscription(directFeedUrl)
                    directFeedUrl = ""
                },
                onQueryChange = viewModel::setQuery,
                onSearch = viewModel::search,
                onPodcastClick = {
                    podcastDetailFromSubscriptions = true
                    podcastDetailPageId = it
                    selectedPodcastId = it
                    topLevelBackStack.clear()
                    topLevelBackStack.add(PodcastNavigationKey.Subscriptions)
                    topLevelBackStack.add(PodcastNavigationKey.PodcastDetail(it))
                },
                onRemovePodcast = undoActions::requestPodcastRemoval,
                onSearchResultClick = { feedUrl -> enqueueFeedSubscription(feedUrl) },
                onOpenSearch = ::openGlobalSearch,
                onImportOpml = platformActions.importOpml,
                importProgress = state.importProgress,
                popularPodcasts = if (selectedSearchProvider?.supportsPopularPodcasts == true) {
                    state.discovery.firstOrNull()?.results.orEmpty()
                } else {
                    emptyList()
                },
                isLoadingPopular = selectedSearchProvider?.supportsPopularPodcasts == true && state.isLoadingDiscovery,
                onOpenPopularPodcast = { result -> openDiscoveryPodcast(result) },
                selectedCategory = gettingStartedCategoryId?.let { id ->
                    PodcastDiscoveryCategories.firstOrNull { it.id == id }
                },
                categoryResults = if (gettingStartedCategoryId != null &&
                    state.categoryDiscovery?.title == PodcastDiscoveryCategories
                        .firstOrNull { it.id == gettingStartedCategoryId }
                        ?.title
                ) {
                    state.categoryDiscovery?.results.orEmpty()
                } else {
                    emptyList()
                },
                categoryResultsById = state.categoryDiscoveryCache.mapValues { (_, shelf) -> shelf.results },
                isLoadingCategory = gettingStartedCategoryId != null && state.isLoadingCategory,
                onCategorySelected = { category ->
                    gettingStartedCategoryId = category?.id
                    category?.let(viewModel::loadCategory)
                },
                onPreloadCategory = viewModel::preloadCategory,
                categories = if (selectedSearchProvider?.id == "apple") {
                    PodcastDiscoveryCategories
                } else {
                    emptyList()
                },
                onRefresh = viewModel::refreshSubscriptions,
                refreshing = refreshing,
                modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            @Composable
            fun DetailContent(destination: PodcastNavigationKey) {
                when (destination) {
                    is PodcastNavigationKey.EpisodeDetail -> {
                        val episode = selectedEpisode ?: return
                        // Keep the pager's source stable while this detail destination is open.
                        // Adding an Inbox episode to Up Next also removes it from Inbox; rebuilding
                        // the pager from that changed source would change its page count during
                        // interaction and interrupt scrolling.
                        val swipeEpisodes = remember(destination.origin, episode.id) {
                            val source = when (destination.origin) {
                                EpisodeNavigationOrigin.Inbox -> inboxSnapshot
                                EpisodeNavigationOrigin.UpNext -> queueEpisodes
                                EpisodeNavigationOrigin.Podcast -> visibleSelectedEpisodes
                                else -> emptyList()
                            }
                            source
                                .map { sourceEpisode -> if (sourceEpisode.id == episode.id) episode else sourceEpisode }
                                .takeIf { items -> items.any { it.id == episode.id } }
                                ?: listOf(episode)
                        }
                        PodcastEpisodeSwipePager(
                            episodes = swipeEpisodes,
                            selectedEpisodeId = episode.id,
                            onEpisodeSelected = { nextEpisode ->
                                viewModel.resolveMediaSizes(nextEpisode)
                            },
                            modifier = Modifier.fillMaxSize(),
                        ) { pageEpisode ->
                            var hydratedEpisode by remember(pageEpisode.id) { mutableStateOf(pageEpisode) }
                            LaunchedEffect(pageEpisode.id) {
                                viewModel.episode(pageEpisode.id)?.let { hydratedEpisode = it }
                            }
                            val persistedEpisode by viewModel.observeEpisode(pageEpisode.id)
                                .collectAsStateWithLifecycle(null)
                            val displayEpisode = persistedEpisode ?: hydratedEpisode
                            val searchResult = selectedSearchResult?.takeIf { it.episode.id == displayEpisode.id }
                                ?: globalSearchEpisodes.firstOrNull { it.episode.id == displayEpisode.id }
                            // Downloads can remain after a podcast is unsubscribed, so use the
                            // complete local catalogue for episode metadata and podcast navigation.
                            val pagePodcast = allPodcastsById[displayEpisode.podcastId]
                                ?: podcastsById[displayEpisode.podcastId]
                                ?: selectedPodcast
                            val podcastTitle = pagePodcast?.title ?: searchResult?.podcastTitle.orEmpty()
                            val pagePodcastSubscriptionStage = pagePodcast?.feedUrl
                                ?.let { feedUrl -> subscriptionProgress[feedUrl]?.stage }
                            val pagePodcastIsSubscribed = pagePodcast?.id in podcastsById ||
                                pagePodcastSubscriptionStage?.isAdded == true
                            val pagePodcastSubscriptionLoading = pagePodcastSubscriptionStage?.isInProgress == true
                            PodcastEpisodeContent(
                                episode = displayEpisode,
                                podcastTitle = podcastTitle,
                                podcastArtworkUrl = pagePodcast?.artworkUrl ?: searchResult?.podcastArtworkUrl,
                                isPlaying = playback.episode?.id == displayEpisode.id && playback.isPlaying,
                                isBuffering = playback.episode?.id == displayEpisode.id && playback.isBuffering,
                                onPlay = {
                                    if (playback.episode?.id == displayEpisode.id) playbackViewModel.togglePlayPause()
                                    else playbackViewModel.play(displayEpisode, podcastTitle)
                                },
                                onWatch = displayEpisode.videoUrl?.let {
                                    {
                                        playbackViewModel.watch(displayEpisode, podcastTitle)
                                        showNowPlaying = true
                                    }
                                },
                                onDownload = { viewModel.download(context, displayEpisode.id) },
                                onRemoveDownload = { viewModel.removeDownload(context, displayEpisode.id) },
                                downloadProgress = downloadProgress[displayEpisode.id],
                                isInQueue = displayEpisode.id in queueEpisodeIds,
                                onToggleQueue = {
                                    if (displayEpisode.id in queueEpisodeIds) {
                                        viewModel.removeFromQueue(displayEpisode.id)
                                    } else {
                                        viewModel.addToQueueFromInbox(
                                            displayEpisode.id,
                                            onError = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        addUpNextFailed,
                                                    )
                                                }
                                            },
                                        )
                                    }
                                },
                                isFavorite = displayEpisode.favorite,
                                onToggleFavorite = { viewModel.toggleFavorite(displayEpisode.id) },
                                onOpenPodcast = {
                                    selectedEpisodeId = null
                                    selectedPodcastId = displayEpisode.podcastId
                                    if (topLevelBackStack.lastOrNull() !is PodcastNavigationKey.PodcastDetail) {
                                        topLevelBackStack.add(
                                            PodcastNavigationKey.PodcastDetail(displayEpisode.podcastId),
                                        )
                                    }
                                },
                                isSubscribed = pagePodcastIsSubscribed,
                                isSubscriptionLoading = pagePodcastSubscriptionLoading,
                                onSubscribe = pagePodcast?.takeIf { !pagePodcastIsSubscribed }?.let { podcast ->
                                    {
                                        enqueueSubscription(
                                            PodcastSearchResult(
                                                providerId = "local",
                                                podcast = com.shapeshed.booth.data.Podcast(
                                                    id = podcast.id,
                                                    title = podcast.title,
                                                    author = podcast.author,
                                                    feedUrl = podcast.feedUrl,
                                                    siteUrl = podcast.siteUrl,
                                                    descriptionHtml = podcast.descriptionHtml,
                                                    artworkUrl = podcast.artworkUrl,
                                                    explicit = podcast.explicit,
                                                ),
                                            ),
                                        )
                                    }
                                },
                                twoPane = useTwoPaneEpisodeLayout,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    is PodcastNavigationKey.PodcastDetail -> {
                        val podcast = selectedPodcast ?: return
                        val podcastPages = remember(podcastDetailFromSubscriptions, podcast.id) {
                            if (!podcastDetailFromSubscriptions) {
                                listOf(podcast)
                            } else {
                                val ordered = orderPodcasts(
                                    podcasts,
                                    podcastSortOrder,
                                    latestEpisodePublishedAt,
                                )
                                if (ordered.any { it.id == podcast.id }) ordered else ordered + podcast
                            }
                        }
                        PodcastDetailSwipePager(
                            podcasts = podcastPages,
                            selectedPodcastId = podcast.id,
                            onPodcastSelected = { podcastDetailPageId = it.id },
                            modifier = Modifier.fillMaxSize(),
                        ) { pagePodcast ->
                            val pageEpisodes by remember(pagePodcast.id) {
                                viewModel.episodes(pagePodcast.id)
                            }.collectAsStateWithLifecycle(emptyList())
                            LaunchedEffect(pagePodcast.id) {
                                if (!pagePodcast.isSubscribed) viewModel.refreshUnsubscribedPodcast(pagePodcast)
                            }
                            PodcastDetailContent(
                                podcast = pagePodcast,
                                episodes = pageEpisodes,
                                modifier = Modifier.fillMaxSize(),
                                onRefresh = { viewModel.refresh(pagePodcast) },
                                // Discovery downloads refresh their retained feed in the
                                // background on entry; do not present that as a user pull.
                                refreshing = refreshing && pagePodcast.isSubscribed,
                                onPlay = { playbackViewModel.play(it, pagePodcast.title) },
                                activeEpisodeId = playback.episode?.id,
                                isPlaying = playback.isPlaying,
                                isBuffering = playback.isBuffering,
                                positionMs = playback.positionMs,
                                onTogglePlayPause = playbackViewModel::togglePlayPause,
                                showDescription = showPodcastDescription,
                                onShowDescriptionChange = { showPodcastDescription = it },
                                onDownload = { viewModel.download(context, it.id) },
                                isSubscribed = pagePodcast.id in podcastsById,
                                onUnsubscribe = { showUnsubscribeConfirmation = true },
                                onSubscribe = { result -> enqueueSubscription(result) },
                                onTag = { tag ->
                                    podcastDetailFromSubscriptions = false
                                    selectedPodcastId = null
                                    topLevelBackStack.add(
                                        PodcastNavigationKey.DiscoveryCategory(
                                            providerId = "local",
                                            categoryId = tag,
                                            title = tag,
                                        ),
                                    )
                                },
                                downloadProgress = downloadProgress,
                                onEpisodeClick = {
                                    viewModel.resolveMediaSizes(it)
                                    episodeOrigin = EpisodeOrigin.PODCAST
                                    podcastDetailPageId = pagePodcast.id
                                    selectedPodcastId = pagePodcast.id
                                    selectedEpisodeId = it.id
                                    topLevelBackStack.add(
                                        PodcastNavigationKey.EpisodeDetail(
                                            episodeId = it.id,
                                            origin = EpisodeNavigationOrigin.Podcast,
                                        ),
                                    )
                                },
                                onEpisodeLongPress = { episode ->
                                    pendingEpisodeAction = createSubscribedEpisodeAction(
                                        episode = episode,
                                        podcastsById = podcastsById,
                                        downloadProgress = downloadProgress,
                                        queueEpisodeIds = queueEpisodeIds,
                                    )
                                },
                            )
                        }
                    }
                    else -> Unit
                }
            }
            @Composable
            fun SpecialRootContent(destination: PodcastNavigationKey) {
                PodcastHomeSpecialRootDestination(
                    destination = destination,
                    podcasts = podcasts,
                    allPodcastsById = allPodcastsById,
                    downloadAssets = downloadAssets,
                    downloadedEpisodes = downloadedEpisodes,
                    playback = playback,
                    downloadProgress = downloadProgress,
                    availableTags = (availablePodcastTags + derived.availableAppleCategories).distinct().sorted(),
                    selectedTags = allEpisodeTags,
                    onSelectedTagsChange = { allEpisodeTags = it },
                    viewModel = viewModel,
                    playbackViewModel = playbackViewModel,
                    context = context,
                    refreshing = refreshing,
                    onOpen = { episode, origin ->
                        episodeOrigin = when (origin) {
                            EpisodeNavigationOrigin.Downloads -> EpisodeOrigin.DOWNLOADS
                            EpisodeNavigationOrigin.AllEpisodes -> EpisodeOrigin.ALL_EPISODES
                            else -> EpisodeOrigin.PODCAST
                        }
                        selectedPodcastId = episode.podcastId
                        viewModel.resolveMediaSizes(episode)
                        selectedEpisodeId = episode.id
                        topLevelBackStack.add(PodcastNavigationKey.EpisodeDetail(episode.id, origin))
                    },
                    onAction = { episode ->
                        pendingEpisodeAction = createSubscribedEpisodeAction(
                            episode = episode,
                            podcastsById = podcastsById,
                            downloadProgress = downloadProgress,
                            queueEpisodeIds = queueEpisodeIds,
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            NavDisplay(
                backStack = topLevelBackStack,
                onBack = {
                    if (topLevelBackStack.size > 1) {
                        popTopLevelRoute()
                        showPodcastDescription = false
                    }
                },
                sceneStrategies = listOf(listDetailStrategy),
                entryProvider = entryProvider {
                    entry<PodcastNavigationKey.GlobalSearch> { globalSearchResults() }
                    entry<PodcastNavigationKey.Settings> { SettingsContent() }
                    entry<PodcastNavigationKey.PodcastManagement> { destination ->
                        PodcastHomeManagementDestination(
                            destination = destination,
                            podcasts = allPodcasts,
                            globalPlaybackSpeed = globalPlaybackSpeed,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    entry<PodcastNavigationKey.Discovery> { DiscoveryContent() }
                    entry<PodcastNavigationKey.DiscoveryPodcast> { DiscoveryContent() }
                    entry<PodcastNavigationKey.DiscoveryCategory> { DiscoveryContent() }
                    entry<PodcastNavigationKey.DiscoveryEpisode> { DiscoveryContent() }
                    entry<PodcastNavigationKey.Inbox>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = { PodcastEmptyDetailPlaceholder() },
                        ),
                    ) { RootTabContent(PodcastTab.HOME) }
                    entry<PodcastNavigationKey.UpNext>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = { PodcastEmptyDetailPlaceholder() },
                        ),
                    ) { RootTabContent(PodcastTab.UP_NEXT) }
                    entry<PodcastNavigationKey.Subscriptions>(
                        metadata = if ((podcasts.isEmpty() || forceGettingStarted) && !showSearch) {
                            emptyMap()
                        } else {
                            ListDetailSceneStrategy.listPane(
                                detailPlaceholder = { PodcastEmptyDetailPlaceholder() },
                            )
                        },
                    ) { RootTabContent(PodcastTab.SUBSCRIPTIONS) }
                    entry<PodcastNavigationKey.Downloads>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = { PodcastEmptyDetailPlaceholder() },
                        ),
                    ) { SpecialRootContent(PodcastNavigationKey.Downloads) }
                    entry<PodcastNavigationKey.AllEpisodes>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = { PodcastEmptyDetailPlaceholder() },
                        ),
                    ) { SpecialRootContent(PodcastNavigationKey.AllEpisodes) }
                    entry<PodcastNavigationKey.PodcastDetail>(
                        metadata = ListDetailSceneStrategy.detailPane(),
                    ) { destination -> DetailContent(destination) }
                    entry<PodcastNavigationKey.PodcastSettings>(
                        metadata = ListDetailSceneStrategy.detailPane(),
                    ) { destination ->
                        allPodcastsById[destination.podcastId]?.let { podcast ->
                            PodcastSettingsContent(
                                podcast = podcast,
                                globalPlaybackSpeed = globalPlaybackSpeed,
                                onPlaybackSpeedChange = { speed ->
                                    viewModel.setPodcastPlaybackSpeed(podcast.id, speed)
                                },
                                videoDownloadsEnabled = podcastDownloadVideos,
                                onVideoDownloadsEnabledChange = viewModel::setPodcastDownloadVideos,
                                globalAutoRefreshEnabled = podcastAutoRefreshEnabled,
                                globalAutoDownloadEnabled = podcastAutoDownloadEnabled,
                                globalAutoQueueEnabled = podcastAutoQueueEnabled,
                                globalNotificationsEnabled = podcastNotificationsEnabled &&
                                    platformActions.notificationsPermissionGranted,
                                availableTags = availablePodcastTags,
                                onSaveSettings = { tags, start, end, refresh, download, queue, notify ->
                                    viewModel.updatePodcastSettings(
                                        podcast.id,
                                        tags,
                                        start,
                                        end,
                                        refresh,
                                        download,
                                        queue,
                                        notify,
                                    )
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    entry<PodcastNavigationKey.EpisodeDetail>(
                        metadata = ListDetailSceneStrategy.detailPane(),
                    ) { destination -> DetailContent(destination) }
                },
            )
        }
        PodcastHomeMiniPlayerOverlay(
            visible = miniPlayerVisible && !showPodcastAppSettings,
            episode = playback.episode,
            podcastTitle = playingPodcastTitle,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            playbackViewModel = playbackViewModel,
            onOpen = { showNowPlaying = true },
            onDismiss = ::dismissNowPlaying,
            onHeightChanged = { miniPlayerHeightPx = it },
        )
        PodcastHomeSecondaryOverlays(
            routeState = routeState,
            homeState = state,
            selectedPodcast = selectedPodcast,
            discoveryDescriptionBlocks = discoveryDescriptionBlocks,
            viewModel = viewModel,
            undoActions = undoActions,
            onUnsubscribeConfirmed = { retainedPodcastAfterUnsubscribe = it },
            directFeedUrl = directFeedUrl,
            onDirectFeedUrlChange = { directFeedUrl = it },
            onAddPodcast = {
                enqueueFeedSubscription(directFeedUrl)
                directFeedUrl = ""
                routeState.showAddPodcast.value = false
            },
        )
        PodcastHomeEpisodeActionsOverlay(
            routeState = routeState,
            selectedTab = selectedTab,
            context = context,
            viewModel = viewModel,
            playbackViewModel = playbackViewModel,
            scope = scope,
            snackbarHostState = snackbarHostState,
        )
    }
}
}
}
}
        PodcastHomeNavigationRail(
            visible = useNavigationRail,
            selectedTab = selectedTab,
            inboxCount = inbox.itemCount,
            onTabSelected = ::selectTabFromHome,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        PodcastHomeNowPlayingOverlay(
            visible = showNowPlaying,
            playback = playback,
            sleepTimer = homeUiState.sleepTimer,
            podcastTitle = playingPodcastTitle,
            onOpenPodcast = { podcastId ->
                showNowPlaying = false
                podcastDetailFromSubscriptions = true
                podcastDetailPageId = podcastId
                selectedPodcastId = podcastId
                topLevelBackStack.clear()
                topLevelBackStack.add(selectedTab.toNavigationKey())
                topLevelBackStack.add(PodcastNavigationKey.PodcastDetail(podcastId))
            },
            playbackViewModel = playbackViewModel,
            viewModel = viewModel,
            queueEpisodes = queueEpisodes,
            podcastsById = allPodcastsById,
            podcastTitlesById = podcastTitlesById,
            onDismiss = ::dismissNowPlaying,
        )
}
}
