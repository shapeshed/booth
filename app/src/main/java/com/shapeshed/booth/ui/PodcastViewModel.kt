package com.shapeshed.booth.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.Podcast
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.QueueEntity
import com.shapeshed.booth.data.Episode
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.PodcastSettingsState
import com.shapeshed.booth.data.PodcastCatalogIndex
import com.shapeshed.booth.data.buildPodcastCatalogIndex
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.downloadEpisodeIds
import com.shapeshed.booth.data.orderedQueueEpisodes
import com.shapeshed.booth.data.PodcastSearchProvider
import com.shapeshed.booth.data.PodcastSearchCatalog
import com.shapeshed.booth.data.PodcastSearchResult
import com.shapeshed.booth.data.PodcastDiscoveryCatalog
import com.shapeshed.booth.data.PodcastDiscoveryProvider
import com.shapeshed.booth.data.DefaultPodcastDiscoveryCatalog
import com.shapeshed.booth.data.PodcastIndexCredentials
import com.shapeshed.booth.data.PodcastIndexCredentialsStore
import com.shapeshed.booth.data.PodcastDiscoveryShelfResult
import com.shapeshed.booth.data.PodcastDiscoveryShelf
import com.shapeshed.booth.data.PodcastFeed
import com.shapeshed.booth.data.PodcastDiscoveryCategory
import com.shapeshed.booth.data.PodcastSubscriptionsViewMode
import com.shapeshed.booth.data.SettingsStore
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.DownloadProgressStore
import com.shapeshed.booth.data.mergeDownloadProgress
import kotlinx.coroutines.flow.MutableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shapeshed.booth.data.EPISODE_ID_INPUT
import com.shapeshed.booth.data.EpisodeDownloadWorker
import com.shapeshed.booth.data.PodcastSubscribeWorker
import com.shapeshed.booth.data.SUBSCRIBE_FEED_URL_INPUT
import com.shapeshed.booth.data.SUBSCRIBE_TITLE_INPUT
import com.shapeshed.booth.data.SUBSCRIBE_DESCRIPTION_INPUT
import com.shapeshed.booth.data.SUBSCRIBE_APPLE_CATEGORIES_INPUT
import com.shapeshed.booth.data.SUBSCRIBE_APPLE_CATEGORY_IDS_INPUT
import com.shapeshed.booth.data.SUBSCRIBE_CATEGORY_PROVIDER_ID_INPUT
import com.shapeshed.booth.data.APPLE_DIRECTORY_PROVIDER_ID
import com.shapeshed.booth.data.podcastId
import com.shapeshed.booth.data.PodcastSubscriptionProgressStore
import com.shapeshed.booth.data.PodcastSubscriptionProgress
import com.shapeshed.booth.data.PodcastSubscriptionStage
import com.shapeshed.booth.data.encodeAppleCategories
import com.shapeshed.booth.data.encodeAppleCategoryIds
import com.shapeshed.booth.data.searchableCategories
import com.shapeshed.booth.data.CategoryEntity
import java.util.concurrent.TimeUnit
import java.util.UUID

data class PodcastHomeState(
    val query: String = "",
    val providerId: String = "apple",
    val results: List<PodcastSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isImporting: Boolean = false,
    val importProgress: PodcastImportProgress? = null,
    val discovery: List<PodcastDiscoveryShelfResult> = emptyList(),
    val isLoadingDiscovery: Boolean = false,
    val discoveryLoaded: Boolean = false,
    val categoryDiscovery: PodcastDiscoveryShelfResult? = null,
    val categoryDiscoveryCache: Map<String, PodcastDiscoveryShelfResult> = emptyMap(),
    val categoryDiscoveryCategory: PodcastDiscoveryCategory? = null,
    val isLoadingCategory: Boolean = false,
    val isLoadingMoreCategory: Boolean = false,
    val hasMoreCategory: Boolean = true,
    val categoryFromPreview: Boolean = false,
    val previewResult: PodcastSearchResult? = null,
    val previewFeed: PodcastFeed? = null,
    val isLoadingPreview: Boolean = false,
    val previewEpisode: com.shapeshed.booth.data.Episode? = null,
    val error: PodcastUiError? = null,
)

private const val OPML_IMPORT_WORK_NAME = "podcast-opml-import"

data class PodcastImportProgress(
    val completed: Int = 0,
    val total: Int = 0,
)

data class PodcastRefreshProgress(
    val completed: Int = 0,
    val total: Int = 0,
) {
    val fraction: Float
        get() = if (total > 0) (completed.toFloat() / total).coerceIn(0f, 1f) else 0f
}

data class PodcastGlobalSearchState(
    val remotePodcasts: List<PodcastSearchResult> = emptyList(),
    val isLoadingRemote: Boolean = false,
    val error: PodcastUiError? = null,
)

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val searchCatalog: PodcastSearchCatalog,
    private val discoveryProviders: @JvmSuppressWildcards List<PodcastDiscoveryProvider>,
    private val settings: SettingsStore,
    private val credentialsStore: PodcastIndexCredentialsStore,
) : ViewModel() {
    private val mediaSizeChecks = ConcurrentHashMap.newKeySet<Long>()
    private var downloadSyncJob: Job? = null
    private var globalSearchJob: Job? = null
    private var discoveryJob: Job? = null
    private var previewJob: Job? = null
    private val categoryJobs = mutableMapOf<String, Deferred<PodcastDiscoveryShelfResult>>()
    val searchProviders: List<PodcastSearchProvider> = searchCatalog.providers
    val podcastIndexCredentials: StateFlow<PodcastIndexCredentials?> = credentialsStore.credentials
    val podcasts: StateFlow<List<PodcastEntity>> = repository.podcasts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val allPodcasts: StateFlow<List<PodcastEntity>> = repository.allPodcasts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val latestEpisodePublishedAt: StateFlow<Map<Long, Long?>> = repository.latestEpisodePublishedAt.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyMap(),
    )
    val podcastCatalogIndex: StateFlow<PodcastCatalogIndex> = repository.podcasts
        .map(::buildPodcastCatalogIndex)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildPodcastCatalogIndex(emptyList()))
    val podcastSettings: StateFlow<PodcastSettingsState> = combine(
        settings.podcastDownloadVideos,
        settings.podcastAutoRefreshEnabled,
        settings.podcastRefreshInterval,
        settings.podcastRefreshNetwork,
        settings.podcastNotificationsEnabled,
    ) { downloadVideos, autoRefreshEnabled, refreshInterval, refreshNetwork, notificationsEnabled ->
        PodcastSettingsState(
            downloadVideos = downloadVideos,
            autoRefreshEnabled = autoRefreshEnabled,
            refreshInterval = refreshInterval,
            refreshNetwork = refreshNetwork,
            notificationsEnabled = notificationsEnabled,
            autoDownloadEnabled = false,
        )
    }
        .combine(settings.podcastAutoDownloadEnabled) { state, autoDownloadEnabled ->
            state.copy(autoDownloadEnabled = autoDownloadEnabled)
        }
        .combine(settings.podcastAutoQueueEnabled) { state, autoQueueEnabled ->
            state.copy(autoQueueEnabled = autoQueueEnabled)
        }
        .combine(settings.podcastDownloadNetwork) { state, downloadNetwork ->
            state.copy(downloadNetwork = downloadNetwork)
        }
        .combine(settings.podcastSelectedTab) { state, savedPodcastTab ->
            state.copy(savedPodcastTab = savedPodcastTab)
        }
        .combine(settings.podcastSearchProvider) { state, searchProviderId ->
            state.copy(searchProviderId = searchProviderId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PodcastSettingsState())
    val podcastPlaybackSpeed: StateFlow<Float> = settings.podcastPlaybackSpeed.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        1f,
    )

    fun setDownloadSyncEnabled(context: Context, enabled: Boolean) {
        if (!enabled) {
            downloadSyncJob?.cancel()
            downloadSyncJob = null
            return
        }
        if (downloadSyncJob?.isActive == true) return
        downloadSyncJob = viewModelScope.launch {
            while (isActive) {
                syncDownloads(context)
                delay(1_000L)
            }
        }
    }
    val inboxPager: Flow<PagingData<EpisodeEntity>> = repository.inboxPager().cachedIn(viewModelScope)
    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val globalSearchEpisodes: StateFlow<List<com.shapeshed.booth.data.PodcastEpisodeSearchResult>> =
        _globalSearchQuery
            .debounce(180)
            .flatMapLatest { repository.searchEpisodes(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _globalSearchState = MutableStateFlow(PodcastGlobalSearchState())
    val globalSearchState: StateFlow<PodcastGlobalSearchState> = _globalSearchState.asStateFlow()

    fun setGlobalSearchQuery(query: String) {
        _globalSearchQuery.value = query
        globalSearchJob?.cancel()
        if (query.trim().length < 2 || selectedSearchProvider() == null) {
            _globalSearchState.value = PodcastGlobalSearchState()
            return
        }
        globalSearchJob = viewModelScope.launch {
            delay(300)
            _globalSearchState.value = _globalSearchState.value.copy(isLoadingRemote = true, error = null)
            runCancellableCatching {
                searchCatalog.search(selectedSearchProvider()!!.id, query.trim())
            }.onSuccess { results ->
                _globalSearchState.value = PodcastGlobalSearchState(remotePodcasts = results)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _globalSearchState.value = PodcastGlobalSearchState(error = PodcastUiError.SearchFailed)
            }
        }
    }

    fun clearGlobalSearch() = setGlobalSearchQuery("")

    private fun selectedDiscoveryCatalog(): PodcastDiscoveryCatalog = DefaultPodcastDiscoveryCatalog(
        discoveryProviders.firstOrNull { it.id == podcastSettings.value.searchProviderId }
            ?: discoveryProviders.first(),
    )

    /**
     * Global search depends on the catalog abstraction rather than a named provider. A
     * Podcast Index or other provider can be added without changing the search UI/state.
     */
    private fun selectedSearchProvider(): PodcastSearchProvider? =
        searchProviders.firstOrNull { it.id == podcastSettings.value.searchProviderId }
            ?: searchProviders.firstOrNull()

    fun setPodcastSearchProvider(providerId: String) {
        if (providerId !in searchProviders.map(PodcastSearchProvider::id)) return
        globalSearchJob?.cancel()
        _globalSearchState.value = PodcastGlobalSearchState()
        _state.value = _state.value.copy(
            discovery = emptyList(),
            isLoadingDiscovery = false,
            discoveryLoaded = false,
            error = null,
        )
        viewModelScope.launch {
            settings.setPodcastSearchProvider(providerId)
            loadDiscovery(force = true)
            if (_globalSearchQuery.value.trim().length >= 2) {
                settings.podcastSearchProvider.first { it == providerId }
                setGlobalSearchQuery(_globalSearchQuery.value)
            }
        }
    }

    fun setPodcastIndexCredentials(apiKey: String, apiSecret: String) {
        credentialsStore.setCredentials(apiKey, apiSecret)
        loadDiscovery(force = true)
    }

    fun clearPodcastIndexCredentials() {
        credentialsStore.clear()
        loadDiscovery(force = true)
    }
    fun allEpisodes(podcastIds: List<Long>?): Flow<PagingData<EpisodeEntity>> =
        if (podcastIds != null && podcastIds.isEmpty()) flowOf(PagingData.empty())
        else repository.allEpisodesPager(podcastIds).cachedIn(viewModelScope)
    val queue: StateFlow<List<QueueEntity>> = repository.queue.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    val queueEpisodes: StateFlow<List<EpisodeEntity>> = repository.queue
        .flatMapLatest { entries ->
            if (entries.isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.episodesByIds(entries.map(QueueEntity::episodeId)).map { episodesById ->
                    orderedQueueEpisodes(entries, episodesById)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val downloadAssets = repository.downloadAssets.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadedEpisodes: StateFlow<Map<Long, EpisodeEntity>> = repository.downloadAssets
        .map(::downloadEpisodeIds)
        .flatMapLatest { episodeIds ->
            if (episodeIds.isEmpty()) emptyFlow() else repository.episodesByIds(episodeIds)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val downloadProgress: StateFlow<Map<Long, DownloadProgress>> = combine(
        downloadAssets,
        DownloadProgressStore.progress,
    ) { assets, liveProgress -> mergeDownloadProgress(assets, liveProgress) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _state = MutableStateFlow(PodcastHomeState())
    val state: StateFlow<PodcastHomeState> = _state.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    val previewEpisodeEntities: StateFlow<Map<Long, EpisodeEntity>> = state
        .map { homeState -> homeState.previewFeed?.episodes?.map { it.id }.orEmpty() }
        .flatMapLatest { episodeIds ->
            if (episodeIds.isEmpty()) emptyFlow() else repository.episodesByIds(episodeIds)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private val _refreshProgress = MutableStateFlow(PodcastRefreshProgress())
    val refreshProgress: StateFlow<PodcastRefreshProgress> = _refreshProgress.asStateFlow()
    val subscriptionsViewMode: StateFlow<PodcastSubscriptionsViewMode> = settings.podcastSubscriptionsViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PodcastSubscriptionsViewMode.LIST)

    fun setSubscriptionsViewMode(mode: PodcastSubscriptionsViewMode) {
        viewModelScope.launch { settings.setPodcastSubscriptionsViewMode(mode) }
    }

    fun setPodcastNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setPodcastNotificationsEnabled(enabled)
            repository.setAllPodcastNotifications(enabled)
        }
    }

    fun setPodcastSelectedTab(tabName: String) {
        viewModelScope.launch { settings.setPodcastSelectedTab(tabName) }
    }

    fun setPodcastDownloadVideos(enabled: Boolean) {
        viewModelScope.launch {
            settings.setPodcastDownloadVideos(enabled)
            repository.setAllPodcastVideoDownload(enabled)
        }
    }

    fun setPodcastAutoRefreshEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setPodcastAutoRefreshEnabled(enabled)
            repository.setAllPodcastAutoRefresh(enabled)
        }
    }

    fun setPodcastRefreshInterval(interval: com.shapeshed.booth.data.PodcastRefreshInterval) {
        viewModelScope.launch { settings.setPodcastRefreshInterval(interval) }
    }

    fun setPodcastRefreshNetwork(network: com.shapeshed.booth.data.PodcastRefreshNetwork) {
        viewModelScope.launch { settings.setPodcastRefreshNetwork(network) }
    }

    fun setPodcastAutoDownloadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setPodcastAutoDownloadEnabled(enabled)
            repository.setAllPodcastAutoDownload(enabled)
        }
    }

    fun setPodcastAutoQueueEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setPodcastAutoQueueEnabled(enabled)
        }
    }

    fun setPodcastDownloadNetwork(network: com.shapeshed.booth.data.PodcastDownloadNetwork) {
        viewModelScope.launch { settings.setPodcastDownloadNetwork(network) }
    }

    fun setPodcastAutoQueue(podcastId: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setPodcastAutoQueue(podcastId, enabled) }
    }

    fun setAllPodcastAutoQueue(enabled: Boolean) {
        viewModelScope.launch { repository.setAllPodcastAutoQueue(enabled) }
    }

    fun resolveMediaSizes(
        episode: EpisodeEntity,
        onResolved: ((Pair<Long?, Long?>) -> Unit)? = null,
    ) {
        if ((episode.audioSizeBytes != null || episode.audioSizeChecked) &&
            (episode.videoUrl.isNullOrBlank() || episode.videoSizeBytes != null || episode.videoSizeChecked)
        ) {
            onResolved?.invoke(episode.audioSizeBytes to episode.videoSizeBytes)
            return
        }
        if (!mediaSizeChecks.add(episode.id)) return
        viewModelScope.launch {
            runCancellableCatching { repository.resolveMediaSizes(episode) }
                .onSuccess { onResolved?.invoke(it) }
                .onFailure { mediaSizeChecks.remove(episode.id) }
        }
    }

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query, error = null)
    }

    fun loadDiscovery(force: Boolean = false) {
        if (!force && (state.value.isLoadingDiscovery || state.value.discoveryLoaded)) return
        if (force) {
            discoveryJob?.cancel()
            _state.value = state.value.copy(
                discovery = emptyList(),
                discoveryLoaded = false,
                isLoadingDiscovery = false,
                error = null,
            )
        }
        // Publish loading before launching so concurrent startup effects cannot enqueue a second
        // request during the small window before the coroutine begins executing.
        _state.value = state.value.copy(isLoadingDiscovery = true, error = null)
        discoveryJob = viewModelScope.launch {
            runCancellableCatching { selectedDiscoveryCatalog().load() }
                .onSuccess { shelves ->
                    _state.value = state.value.copy(
                        discovery = shelves,
                        discoveryLoaded = true,
                    )
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _state.value = state.value.copy(error = PodcastUiError.DiscoveryLoadFailed)
                }
            _state.value = state.value.copy(isLoadingDiscovery = false)
        }
    }

    fun loadCategory(category: PodcastDiscoveryCategory) {
        val catalog = selectedDiscoveryCatalog()
        val cached = state.value.categoryDiscoveryCache[category.id]
        if (cached != null) {
            _state.value = state.value.copy(
                categoryDiscovery = cached,
                categoryDiscoveryCategory = category,
                isLoadingCategory = false,
                hasMoreCategory = catalog.supportsCategoryPaging,
                categoryFromPreview = state.value.previewResult != null,
                error = null,
            )
            return
        }
        viewModelScope.launch {
            _state.value = state.value.copy(
                // Publish the destination before starting the request so the category screen
                // appears immediately while results load.
                categoryDiscovery = PodcastDiscoveryShelfResult(
                    shelf = PodcastDiscoveryShelf.TOP_SHOWS,
                    results = emptyList(),
                    title = category.title,
                ),
                categoryDiscoveryCategory = category,
                isLoadingCategory = true,
                isLoadingMoreCategory = false,
                hasMoreCategory = catalog.supportsCategoryPaging,
                categoryFromPreview = state.value.previewResult != null,
                error = null,
            )
            runCancellableCatching { categoryRequest(category).await() }
                .onSuccess { result ->
                    _state.value = state.value.copy(
                        categoryDiscovery = result,
                        categoryDiscoveryCategory = category,
                        categoryDiscoveryCache = state.value.categoryDiscoveryCache + (category.id to result),
                        isLoadingCategory = false,
                    )
                }
                .onFailure {
                    _state.value = state.value.copy(
                        isLoadingCategory = false,
                        error = PodcastUiError.CategoryLoadFailed,
                    )
                }
        }
    }

    fun loadMoreCategory() {
        val category = state.value.categoryDiscoveryCategory ?: return
        val current = state.value.categoryDiscovery ?: return
        if (state.value.isLoadingCategory || state.value.isLoadingMoreCategory || !state.value.hasMoreCategory) return
        val offset = current.results.size
        _state.value = state.value.copy(isLoadingMoreCategory = true, error = null)
        viewModelScope.launch {
            runCancellableCatching { selectedDiscoveryCatalog().load(category, offset) }
                .onSuccess { page ->
                    val results = (current.results + page.results).distinctBy { it.podcast.feedUrl }
                    val combined = current.copy(results = results)
                    _state.value = state.value.copy(
                        categoryDiscovery = combined,
                        categoryDiscoveryCache = state.value.categoryDiscoveryCache + (category.id to combined),
                        isLoadingMoreCategory = false,
                        hasMoreCategory = page.results.isNotEmpty() && results.size > current.results.size,
                    )
                }
                .onFailure {
                    _state.value = state.value.copy(
                        isLoadingMoreCategory = false,
                        error = PodcastUiError.CategoryLoadFailed,
                    )
                }
        }
    }

    fun preloadCategory(category: PodcastDiscoveryCategory) {
        if (state.value.categoryDiscoveryCache.containsKey(category.id)) return
        viewModelScope.launch {
            runCancellableCatching { categoryRequest(category).await() }
                .onSuccess { result ->
                    _state.value = state.value.copy(
                        categoryDiscoveryCache = state.value.categoryDiscoveryCache + (category.id to result),
                    )
                }
        }
    }

    private fun categoryRequest(category: PodcastDiscoveryCategory): Deferred<PodcastDiscoveryShelfResult> =
        categoryJobs.getOrPut(category.id) {
            viewModelScope.async {
                selectedDiscoveryCatalog().load(category)
            }.also { request ->
                request.invokeOnCompletion { categoryJobs.remove(category.id, request) }
            }
        }

    fun loadLocalTag(tag: String) {
        val normalizedTag = tag.trim()
        if (normalizedTag.isBlank()) return
        val results = podcasts.value
            .filter { podcast ->
                podcast.searchableCategories().any { it.equals(normalizedTag, ignoreCase = true) }
            }
            .map { podcast ->
                PodcastSearchResult(
                    providerId = "local",
                    podcast = Podcast(
                        id = podcast.id,
                        title = podcast.title,
                        author = podcast.author,
                        feedUrl = podcast.feedUrl,
                        siteUrl = podcast.siteUrl,
                        descriptionHtml = podcast.descriptionHtml,
                        artworkUrl = podcast.artworkUrl,
                        explicit = podcast.explicit,
                        categories = podcast.categories.map(CategoryEntity::name),
                        categoryIds = podcast.categories.mapNotNull { category ->
                            category.externalId?.let { category.name to it }
                        }.toMap(),
                    ),
                )
            }
        _state.value = state.value.copy(
            previewResult = null,
            previewFeed = null,
            previewEpisode = null,
            categoryDiscovery = PodcastDiscoveryShelfResult(
                shelf = PodcastDiscoveryShelf.TOP_SHOWS,
                results = results,
                title = normalizedTag,
            ),
            categoryDiscoveryCategory = null,
            categoryFromPreview = false,
            hasMoreCategory = false,
            error = null,
        )
    }

    fun closeCategory() {
        _state.value = state.value.copy(
            categoryDiscovery = null,
            categoryDiscoveryCategory = null,
            isLoadingMoreCategory = false,
            hasMoreCategory = false,
            categoryFromPreview = false,
        )
    }

    fun selectSearchProvider(providerId: String) {
        if (providerId !in searchProviders.map(PodcastSearchProvider::id)) return
        _state.value = state.value.copy(
            providerId = providerId,
            results = emptyList(),
            categoryDiscovery = null,
            categoryDiscoveryCache = emptyMap(),
            error = null,
        )
    }

    fun search() {
        val query = state.value.query.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.value = state.value.copy(isSearching = true, error = null)
            runCancellableCatching { searchCatalog.search(state.value.providerId, query) }
                .onSuccess { results -> _state.value = state.value.copy(results = results) }
                .onFailure { _state.value = state.value.copy(error = PodcastUiError.SearchFailed) }
            _state.value = state.value.copy(isSearching = false)
        }
    }

    fun subscribe(
        feedUrl: String,
        fallbackDescriptionHtml: String? = null,
        onSuccess: (PodcastFeed) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCancellableCatching {
                repository.subscribe(
                    feedUrl,
                    fallbackDescriptionHtml = fallbackDescriptionHtml,
                )
            }
                .onSuccess(onSuccess)
                .onFailure { _state.value = state.value.copy(error = PodcastUiError.SubscribeFailed) }
        }
    }

    fun subscribeInBackground(
        context: android.content.Context,
        feedUrl: String,
        title: String? = null,
        fallbackDescriptionHtml: String? = null,
        appleCategories: List<String> = emptyList(),
        appleCategoryIds: Map<String, String> = emptyMap(),
        categoryProviderId: String = APPLE_DIRECTORY_PROVIDER_ID,
    ) {
        PodcastSubscriptionProgressStore.queued(feedUrl, title)
        val request = OneTimeWorkRequestBuilder<PodcastSubscribeWorker>()
            .setInputData(
                workDataOf(
                    SUBSCRIBE_FEED_URL_INPUT to feedUrl,
                    SUBSCRIBE_TITLE_INPUT to title,
                    SUBSCRIBE_DESCRIPTION_INPUT to fallbackDescriptionHtml,
                    SUBSCRIBE_APPLE_CATEGORIES_INPUT to encodeAppleCategories(appleCategories),
                    SUBSCRIBE_APPLE_CATEGORY_IDS_INPUT to encodeAppleCategoryIds(appleCategoryIds),
                    SUBSCRIBE_CATEGORY_PROVIDER_ID_INPUT to categoryProviderId,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "podcast-subscribe-${podcastId(feedUrl)}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun preview(result: PodcastSearchResult) {
        previewJob?.cancel()
        _state.value = state.value.copy(
            previewResult = result,
            previewFeed = null,
            previewEpisode = null,
            categoryDiscovery = null,
            isLoadingPreview = true,
            categoryFromPreview = false,
            error = null,
        )
        previewJob = viewModelScope.launch {
            val provider = selectedSearchProvider()
                ?: searchProviders.firstOrNull { it.id == result.providerId }
            val cachedFeed = repository.previewCached(result.podcast.feedUrl)
            if (cachedFeed != null) {
                _state.value = state.value.copy(
                    previewFeed = cachedFeed,
                    isLoadingPreview = false,
                )
            }
            coroutineScope {
                // Feed loading is the critical path for the episode list. Provider enrichment
                // adds useful metadata, but must not serialize another network request ahead of it.
                val feedJob = launch {
                    try {
                        repository.previewStreaming(result.podcast.feedUrl) { partialFeed ->
                            if (isActive) {
                                _state.value = state.value.copy(
                                    previewFeed = partialFeed,
                                    error = null,
                                )
                            }
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        if (cachedFeed == null) {
                            _state.value = state.value.copy(error = PodcastUiError.PodcastLoadFailed)
                        }
                    }
                }
                val enrichmentJob = launch {
                    val enrichedResult = if (provider == null || result.providerId == "local") {
                        result
                    } else {
                        try {
                            provider.enrich(result)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            result
                        }
                    }
                    _state.value = state.value.copy(previewResult = enrichedResult)
                }
                feedJob.join()
                enrichmentJob.join()
            }
            _state.value = state.value.copy(isLoadingPreview = false)
        }
    }

    fun closePreview() {
        previewJob?.cancel()
        previewJob = null
        _state.value = state.value.copy(previewResult = null, previewFeed = null, previewEpisode = null, isLoadingPreview = false)
    }

    fun openPreviewEpisode(episode: com.shapeshed.booth.data.Episode) {
        _state.value = state.value.copy(previewEpisode = episode)
    }

    fun closePreviewEpisode() {
        _state.value = state.value.copy(previewEpisode = null)
    }

    fun preparePreviewEpisode(
        episode: Episode,
        podcast: Podcast? = null,
        onReady: (com.shapeshed.booth.data.EpisodeEntity) -> Unit,
    ) {
        viewModelScope.launch {
            runCancellableCatching { repository.savePreviewEpisode(episode, podcast) }
                .onSuccess(onReady)
                .onFailure { error ->
                    _state.value = state.value.copy(error = PodcastUiError.EpisodeLoadFailed)
                }
        }
    }

    fun downloadPreview(context: android.content.Context, episode: Episode, podcast: Podcast? = null) {
        preparePreviewEpisode(episode, podcast) { savedEpisode -> download(context, savedEpisode.id) }
    }

    fun addPreviewToQueue(
        episode: Episode,
        podcast: Podcast? = null,
        onAdded: () -> Unit = {},
        onError: () -> Unit = {},
    ) {
        preparePreviewEpisode(episode, podcast) { savedEpisode ->
            viewModelScope.launch {
                runCancellableCatching { repository.addToQueueFromInbox(savedEpisode.id) }
                    .onSuccess { onAdded() }
                    .onFailure { onError() }
            }
        }
    }

    fun importOpml(context: Context, uri: Uri) {
        viewModelScope.launch {
            val body = runCancellableCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    }
                }
            }.getOrElse {
                _state.value = state.value.copy(error = PodcastUiError.OpmlReadFailed)
                return@launch
            }
            if (body == null) {
                _state.value = state.value.copy(error = PodcastUiError.OpmlReadFailed)
                return@launch
            }
            val fileName = "opml-import-${UUID.randomUUID()}.xml"
            runCancellableCatching {
                context.openFileOutput(fileName, Context.MODE_PRIVATE).bufferedWriter().use { writer ->
                    writer.write(body)
                }
                val request = OneTimeWorkRequestBuilder<com.shapeshed.booth.data.PodcastOpmlImportWorker>()
                    .setInputData(workDataOf(com.shapeshed.booth.data.OPML_IMPORT_FILE_INPUT to fileName))
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
                _state.value = state.value.copy(isImporting = true, importProgress = null, error = null)
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    OPML_IMPORT_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
                observeOpmlImport(context, request.id)
            }.onFailure {
                _state.value = state.value.copy(error = PodcastUiError.OpmlReadFailed)
            }
        }
    }

    private suspend fun observeOpmlImport(context: Context, workId: UUID) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        lateinit var terminalInfo: androidx.work.WorkInfo
        while (true) {
            val current = withContext(Dispatchers.IO) {
                workManager.getWorkInfoById(workId).get()
            } ?: return
            if (current.state.isFinished) {
                terminalInfo = current
                break
            }
            _state.value = state.value.copy(
                isImporting = true,
                importProgress = PodcastImportProgress(
                    current.progress.getInt(com.shapeshed.booth.data.OPML_IMPORT_COMPLETED, 0),
                    current.progress.getInt(com.shapeshed.booth.data.OPML_IMPORT_TOTAL, 0),
                ),
            )
            delay(250)
        }
        val info = terminalInfo
        val imported = info.outputData.getInt(com.shapeshed.booth.data.OPML_IMPORT_IMPORTED, 0)
        val total = info.outputData.getInt(com.shapeshed.booth.data.OPML_IMPORT_TOTAL, 0)
        _state.value = state.value.copy(
            isImporting = false,
            importProgress = null,
            error = if (info.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                PodcastUiError.ImportCompleted(imported, total)
            } else {
                PodcastUiError.OpmlReadFailed
            },
        )
    }

    fun exportOpml(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCancellableCatching {
                val body = repository.exportOpml()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(body)
                    } ?: error("Could not open export destination")
                }
            }
                .onFailure {
                    _state.value = state.value.copy(error = PodcastUiError.ExportFailed)
                }
        }
    }

    fun refresh(podcast: PodcastEntity) {
        if (_refreshing.value) return
        viewModelScope.launch {
            val existingGuids = repository.episodeGuids(podcast.id).toSet()
            _refreshing.value = true
            _refreshProgress.value = PodcastRefreshProgress(total = 1)
            try {
                repository.refresh(podcast)
                if (settings.podcastAutoQueueEnabled.first()) {
                    repository.addNewEpisodesToQueue(podcast, existingGuids)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _state.value = state.value.copy(error = PodcastUiError.RefreshFailed)
            } finally {
                _refreshProgress.value = PodcastRefreshProgress(completed = 1, total = 1)
                _refreshing.value = false
            }
        }
    }

    /** Loads retained discovery podcast episodes without presenting a user refresh operation. */
    fun refreshUnsubscribedPodcast(podcast: PodcastEntity) {
        viewModelScope.launch {
            runCancellableCatching { repository.refresh(podcast) }
        }
    }

    fun refreshSubscriptions() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val refreshTargets = podcasts.value
            val existingGuids = refreshTargets.associate { it.id to repository.episodeGuids(it.id).toSet() }
            _refreshProgress.value = PodcastRefreshProgress(total = refreshTargets.size)
            try {
                val results = repository.refreshAll(refreshTargets) { completed, total ->
                    _refreshProgress.value = PodcastRefreshProgress(completed, total)
                }
                if (settings.podcastAutoQueueEnabled.first()) {
                    results.filter { it.result.isSuccess }.forEach { result ->
                        repository.addNewEpisodesToQueue(result.podcast, existingGuids[result.podcast.id].orEmpty())
                    }
                }
                results.firstNotNullOfOrNull { it.result.exceptionOrNull() }?.let {
                    _state.value = state.value.copy(error = PodcastUiError.PartialRefreshFailed)
                }
            } finally {
                _refreshProgress.value = PodcastRefreshProgress(
                    completed = refreshTargets.size,
                    total = refreshTargets.size,
                )
                _refreshing.value = false
            }
        }
    }

    fun remove(podcast: PodcastEntity) {
        PodcastSubscriptionProgressStore.update(
            podcast.feedUrl,
            PodcastSubscriptionProgress(
                stage = PodcastSubscriptionStage.UNSUBSCRIBING,
                title = podcast.title,
            ),
        )
        viewModelScope.launch {
            runCancellableCatching { repository.remove(podcast) }
                .onFailure { _state.value = state.value.copy(error = PodcastUiError.RemovePodcastFailed) }
                .also { PodcastSubscriptionProgressStore.clear(podcast.feedUrl) }
        }
    }

    fun markUnsubscribing(podcast: PodcastEntity) {
        PodcastSubscriptionProgressStore.update(
            podcast.feedUrl,
            PodcastSubscriptionProgress(
                stage = PodcastSubscriptionStage.UNSUBSCRIBING,
                title = podcast.title,
            ),
        )
    }

    fun clearSubscriptionProgress(podcast: PodcastEntity) {
        PodcastSubscriptionProgressStore.clear(podcast.feedUrl)
    }

    fun updatePodcastSettings(
        podcastId: Long,
        tags: String,
        skipStartSeconds: Int,
        skipEndSeconds: Int,
        includeInAutoRefresh: Boolean,
        includeInAutoDownload: Boolean,
        includeInAutoQueue: Boolean,
        includeInNotifications: Boolean,
    ) {
        viewModelScope.launch {
            repository.updatePodcastSettings(
                podcastId,
                tags,
                skipStartSeconds,
                skipEndSeconds,
                includeInAutoRefresh,
                includeInAutoDownload,
                includeInAutoQueue,
                includeInNotifications,
            )
        }
    }

    fun setPodcastPlaybackSpeed(podcastId: Long, speed: Float?) {
        viewModelScope.launch {
            repository.setPodcastPlaybackSpeed(podcastId, speed)
        }
    }

    fun setPodcastVideoDownload(podcastId: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setPodcastVideoDownload(podcastId, enabled) }
    }

    fun setAllPodcastAutoRefresh(enabled: Boolean) {
        viewModelScope.launch { repository.setAllPodcastAutoRefresh(enabled) }
    }

    fun setAllPodcastAutoDownload(enabled: Boolean) {
        viewModelScope.launch { repository.setAllPodcastAutoDownload(enabled) }
    }

    fun setAllPodcastVideoDownload(enabled: Boolean) {
        viewModelScope.launch { repository.setAllPodcastVideoDownload(enabled) }
    }

    fun setAllPodcastNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.setAllPodcastNotifications(enabled) }
    }

    fun episodes(podcastId: Long): Flow<List<com.shapeshed.booth.data.EpisodeEntity>> =
        repository.episodes(podcastId)

    suspend fun episode(episodeId: Long): com.shapeshed.booth.data.EpisodeEntity? =
        repository.episode(episodeId)

    fun observeEpisode(episodeId: Long): Flow<com.shapeshed.booth.data.EpisodeEntity?> =
        repository.observeEpisode(episodeId)

    suspend fun podcast(podcastId: Long): PodcastEntity? = repository.podcast(podcastId)

    fun episodesByIds(episodeIds: List<Long>): Flow<Map<Long, com.shapeshed.booth.data.EpisodeEntity>> =
        repository.episodesByIds(episodeIds)

    fun addToQueueFromInbox(episodeId: Long, onAdded: () -> Unit = {}, onError: () -> Unit = {}) {
        viewModelScope.launch {
            runCancellableCatching { repository.addToQueueFromInbox(episodeId) }
                .onSuccess { onAdded() }
                .onFailure { onError() }
        }
    }

    fun dismissFromInbox(episodeId: Long) {
        viewModelScope.launch { repository.dismissFromInbox(episodeId) }
    }

    fun clearInbox() {
        viewModelScope.launch { repository.clearInbox() }
    }

    fun restoreToInbox(episodeId: Long) {
        viewModelScope.launch { repository.restoreToInbox(episodeId) }
    }

    fun markPlayed(episodeId: Long) {
        viewModelScope.launch { repository.markPlayed(episodeId) }
    }

    fun markUnplayed(episodeId: Long) {
        viewModelScope.launch { repository.markUnplayed(episodeId) }
    }

    fun toggleFavorite(episodeId: Long) {
        viewModelScope.launch { repository.toggleFavorite(episodeId) }
    }

    fun removeFromQueue(episodeId: Long) {
        viewModelScope.launch { repository.removeFromQueue(episodeId) }
    }

    suspend fun removeFromQueueAwait(episodeId: Long): Result<Unit> = withContext(NonCancellable) {
        runCancellableCatching { repository.removeFromQueue(episodeId) }
    }

    fun reorderQueue(episodeIds: List<Long>) {
        viewModelScope.launch { repository.reorderQueue(episodeIds) }
    }

    fun download(context: android.content.Context, episodeId: Long) {
        // WorkManager and DownloadManager are intentionally asynchronous. Publish the
        // pending state now so every list view gives immediate feedback on the tap.
        DownloadProgressStore.request(episodeId)
        val request = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
            .setInputData(workDataOf(EPISODE_ID_INPUT to episodeId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "podcast-download-$episodeId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    suspend fun syncDownloads(context: android.content.Context) = withContext(Dispatchers.IO) {
        com.shapeshed.booth.data.PodcastDownloadManager(
            context.applicationContext,
            repository,
        ).syncActiveDownloads()
    }

    fun removeDownload(context: android.content.Context, episodeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            com.shapeshed.booth.data.PodcastDownloadManager(
                context.applicationContext,
                repository,
            ).removeEpisodeDownloads(episodeId)
        }
    }

}
