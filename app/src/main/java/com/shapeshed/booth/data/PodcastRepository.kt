package com.shapeshed.booth.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class PodcastRefreshResult(val podcast: PodcastEntity, val result: Result<PodcastFeed>)

class PodcastRepository(
    private val feedProvider: PodcastFeedProvider,
    private val dao: PodcastDao,
    private val downloadDao: DownloadAssetDao,
    private val httpClient: OkHttpClient? = null,
    private val streamingFeedProvider: StreamingPodcastFeedProvider? = null,
) {
    private val refreshLocks = ConcurrentHashMap<Long, Mutex>()

    /**
     * Keeps the FTS index in step with the episodes table.
     *
     * This used to hand work to a Channel drained by a coroutine launched from `init` on a
     * `SupervisorJob` that nothing ever cancelled. Three problems came with that:
     *
     *  - Constructing the class spawned a background coroutine touching the database, so no unit
     *    test could build one. That was the main obstacle to testing this repository at all.
     *  - If the process died between the Room write and the channel drain, the index was
     *    permanently missing those episodes for the life of the install, and `rebuildSearchIndex`
     *    was unreachable, so nothing could repair it.
     *  - `Channel.UNLIMITED` let a large refresh accumulate unbounded pending batches in memory.
     *
     * Indexing is now inline and awaited. It is one FTS statement per write, so the cost is small,
     * and in exchange the index is immediately consistent and the class has no hidden lifecycle.
     *
     * Failures stay non-fatal: a feed must still save if the FTS update does not, because Booth is
     * local-first and a broken search is recoverable while a lost subscription is not.
     */
    private suspend fun indexEpisodes(operation: suspend () -> Unit) {
        try {
            operation()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Best effort by design; see above.
        }
    }

    private suspend fun indexUpsert(episodes: List<EpisodeEntity>) = indexEpisodes {
        if (episodes.isNotEmpty()) {
            dao.upsertEpisodeSearch(episodes.map(::toSearchEntity))
        }
    }

    private suspend fun indexDelete(episodeIds: List<Long>) = indexEpisodes {
        if (episodeIds.isNotEmpty()) {
            dao.deleteEpisodeSearch(episodeIds)
        }
    }

    /**
     * Explicit full rebuild for recovery, never for list startup. Normal searches repair only
     * missing entries, retaining the existing index and avoiding a whole-library Kotlin allocation.
     */
    suspend fun rebuildSearchIndex() = indexEpisodes {
        dao.clearEpisodeSearch()
        dao.upsertEpisodeSearch(dao.allEpisodesForSearchIndex().map(::toSearchEntity))
    }

    val podcasts: Flow<List<PodcastEntity>> = dao.observePodcasts().combine(dao.observePodcastCategories()) {
            podcasts,
            categories,
        ->
        podcasts.withCategories(categories)
    }
    val allPodcasts: Flow<List<PodcastEntity>> = dao.observeAllPodcasts().combine(dao.observePodcastCategories()) {
            podcasts,
            categories,
        ->
        podcasts.withCategories(categories)
    }

    /** Snapshot of persisted episodes used by portable backup/export jobs. */
    suspend fun allEpisodesSnapshot(): List<EpisodeEntity> = dao.allEpisodesForSearchIndex()

    suspend fun podcastByFeedUrl(feedUrl: String): PodcastEntity? = dao.podcastByFeedUrl(feedUrl)

    suspend fun upsertBackupPodcast(podcast: PodcastEntity) = dao.upsertPodcast(podcast)

    suspend fun upsertBackupEpisode(episode: EpisodeEntity) = dao.upsertEpisodes(listOf(episode))

    suspend fun restoreBackupPlayback(
        episodeId: Long,
        positionMs: Long,
        completed: Boolean,
        durationMs: Long?,
        favorite: Boolean,
    ) {
        dao.updateProgress(episodeId, positionMs, completed, durationMs)
        dao.setFavorite(episodeId, favorite)
    }

    suspend fun restoreBackupInboxState(episodeId: Long, inInbox: Boolean) = dao.setInbox(episodeId, inInbox)

    val latestEpisodePublishedAt: Flow<Map<Long, Long?>> =
        dao.observeLatestEpisodePublishedAt().map { rows ->
            rows.associate { it.podcastId to it.publishedAtMillis }
        }

    fun inboxPager(): Flow<PagingData<EpisodeEntity>> = Pager(
        config = PagingConfig(pageSize = 40, prefetchDistance = 10, enablePlaceholders = false),
    ) { dao.observeInbox() }.flow

    fun allEpisodesPager(podcastIds: List<Long>? = null): Flow<PagingData<EpisodeEntity>> = Pager(
        config = PagingConfig(pageSize = 40, prefetchDistance = 10, enablePlaceholders = false),
    ) {
        if (podcastIds == null) {
            dao.observeAllEpisodes()
        } else {
            dao.observeEpisodesForPodcasts(podcastIds)
        }
    }.flow

    fun searchEpisodes(query: String, limit: Int = 80): Flow<List<PodcastEpisodeSearchResult>> =
        podcastFtsQuery(query)?.let { ftsQuery ->
            dao.observeEpisodesMatching(ftsQuery, limit).onStart {
                // Older builds and interrupted/best-effort writes can leave missing entries.
                // Repair only when FTS is used; blank queries at home startup need no index.
                // One SQL statement keeps the missing-row check and insert atomic, and a
                // cancelled search cancels its repair rather than leaving detached work behind.
                indexEpisodes { dao.insertMissingEpisodeSearchEntries() }
            }
        }
            ?: if (query.isBlank()) {
                dao.observeRecentEpisodesForSearch(limit.coerceAtMost(40))
            } else {
                flowOf(emptyList())
            }

    val queue: Flow<List<QueueEntity>> = dao.observeQueue()

    val downloadAssets: Flow<List<DownloadAssetEntity>> = downloadDao.observeAll()

    suspend fun downloadAssetsForEpisodes(episodeIds: Collection<Long>): List<DownloadAssetEntity> {
        val assets = mutableListOf<DownloadAssetEntity>()
        episodeIds.distinct().chunked(DOWNLOAD_ASSET_QUERY_BATCH_SIZE).forEach { batch ->
            if (batch.isNotEmpty()) assets += downloadDao.findForEpisodes(batch)
        }
        return assets
    }

    fun episodes(podcastId: Long): Flow<List<EpisodeEntity>> = dao.observeEpisodes(podcastId)

    fun observeEpisode(episodeId: Long): Flow<EpisodeEntity?> = dao.observeEpisode(episodeId)

    fun episodesByIds(episodeIds: List<Long>): Flow<Map<Long, EpisodeEntity>> =
        dao.observeEpisodesByIds(episodeIds).map { rows -> rows.associateBy(EpisodeEntity::id) }

    suspend fun subscribe(
        feedUrl: String,
        importedTags: String? = null,
        fallbackDescriptionHtml: String? = null,
        appleCategories: List<String> = emptyList(),
        appleCategoryIds: Map<String, String> = emptyMap(),
        categoryProviderId: String = APPLE_DIRECTORY_PROVIDER_ID,
        onEpisodeProgress: (processed: Int, total: Int) -> Unit = { _, _ -> },
        onSaving: () -> Unit = {},
    ): PodcastFeed = withContext(Dispatchers.IO) {
        val feed = feedProvider.fetch(
            canonicalFeedUrl(feedUrl),
            onEpisodeProgress = onEpisodeProgress,
        )
        onSaving()
        save(
            feed,
            importedTags = importedTags,
            fallbackDescriptionHtml = fallbackDescriptionHtml,
            appleCategories = appleCategories,
            appleCategoryIds = appleCategoryIds,
            categoryProviderId = categoryProviderId,
            forceSubscribe = true,
        )
        feed
    }

    suspend fun exportOpml(): String = withContext(Dispatchers.IO) {
        buildPodcastOpml(dao.podcasts())
    }

    suspend fun preview(feedUrl: String): PodcastFeed = withContext(Dispatchers.IO) {
        feedProvider.fetch(canonicalFeedUrl(feedUrl))
    }

    suspend fun previewStreaming(feedUrl: String, onUpdate: suspend (PodcastFeed) -> Unit): PodcastFeed =
        streamingFeedProvider?.fetch(feedUrl, onUpdate) ?: preview(feedUrl)

    /**
     * Returns the locally persisted copy of a feed, when discovery has seen it before.
     * This does not update any rows or subscription state.
     */
    suspend fun previewCached(feedUrl: String): PodcastFeed? = withContext(Dispatchers.IO) {
        val canonicalUrl = canonicalFeedUrl(feedUrl)
        val podcast = dao.podcastByFeedUrl(canonicalUrl)
            ?: dao.podcast(podcastId(canonicalUrl))
            ?: return@withContext null
        val episodes = dao.episodesForPodcast(podcast.id)
            .sortedWith(
                compareByDescending<EpisodeEntity> { it.publishedAtMillis ?: Long.MIN_VALUE }
                    .thenByDescending { it.firstSeenAtMillis ?: Long.MIN_VALUE },
            )
        PodcastFeed(
            podcast = enrich(podcast).toPreviewPodcast(),
            episodes = episodes.map(EpisodeEntity::toPreviewEpisode),
            etag = podcast.feedEtag,
            lastModified = podcast.feedLastModified,
        )
    }

    /**
     * Resolve tracking/CDN redirects before handing a media URL to DownloadManager. DownloadManager
     * has a platform redirect limit; OkHttp can follow the longer chains used by some podcast hosts.
     * The returned URL is deliberately not persisted because signed CDN URLs can expire.
     */
    suspend fun resolveDownloadUrl(sourceUrl: String): String = withContext(Dispatchers.IO) {
        val client = httpClient ?: return@withContext sourceUrl
        resolveDownloadUrl(client, sourceUrl)
    }

    private fun resolveDownloadUrl(client: OkHttpClient, sourceUrl: String): String {
        val headRequest = Request.Builder()
            .url(sourceUrl)
            .header("Accept-Encoding", "identity")
            .head()
            .build()
        client.newCall(headRequest).execute().use { response ->
            if (response.isSuccessful) return response.request.url.toString()
            if (response.code != 405 && response.code != 501) {
                error("Media URL resolution failed with HTTP ${response.code}")
            }
        }

        // A few media hosts reject HEAD but support a one-byte range request. The body is closed
        // immediately; this only resolves the redirect chain and does not download the episode.
        val rangeRequest = Request.Builder()
            .url(sourceUrl)
            .header("Accept-Encoding", "identity")
            .header("Range", "bytes=0-0")
            .get()
            .build()
        client.newCall(rangeRequest).execute().use { response ->
            if (!response.isSuccessful) {
                error("Media URL resolution failed with HTTP ${response.code}")
            }
            return response.request.url.toString()
        }
    }

    suspend fun refresh(podcast: PodcastEntity): PodcastFeed = refreshLocks
        .getOrPut(podcast.id) { Mutex() }
        .withLock {
            refreshInternal(podcast)
        }

    /**
     * How often this feed publishes, inferred from its recent episode dates.
     *
     * Reads a handful of timestamps rather than the episode rows, because this runs for every
     * subscription whenever refresh scheduling is reconciled.
     */
    suspend fun typicalPublishIntervalMillis(podcastId: Long): Long? =
        RefreshCadence.typicalIntervalMillis(dao.recentPublishTimes(podcastId, CADENCE_SAMPLE_SIZE))

    /** When this feed should next be fetched, according to its own publishing pattern. */
    suspend fun nextRefreshDueMillis(podcast: PodcastEntity, nowMillis: Long): Long = RefreshCadence.nextDueMillis(
        lastRefreshMillis = podcast.lastRefreshMillis,
        publishedAtMillis = dao.recentPublishTimes(podcast.id, CADENCE_SAMPLE_SIZE),
        nowMillis = nowMillis,
    )

    suspend fun addNewEpisodesToQueue(podcast: PodcastEntity, existingEpisodeIdentities: Set<Pair<String, String>>) {
        if (!podcast.includeInAutoQueue) return
        newEpisodesSince(episodes(podcast.id).first(), existingEpisodeIdentities)
            .forEach { addToQueueFromInbox(it.id) }
    }

    /** Refreshes feeds concurrently while keeping each feed's database merge atomic. */
    suspend fun refreshAll(
        podcasts: List<PodcastEntity>,
        maxConcurrency: Int = 4,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): List<PodcastRefreshResult> = coroutineScope {
        val permits = Semaphore(maxConcurrency.coerceAtLeast(1))
        val completed = java.util.concurrent.atomic.AtomicInteger()
        podcasts.map { podcast ->
            async(Dispatchers.IO) {
                permits.withPermit {
                    try {
                        val result = try {
                            Result.success(refresh(podcast))
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Result.failure(error)
                        }
                        PodcastRefreshResult(podcast, result)
                    } finally {
                        onProgress(completed.incrementAndGet(), podcasts.size)
                    }
                }
            }
        }.awaitAll()
    }

    private suspend fun refreshInternal(podcast: PodcastEntity): PodcastFeed = withContext(Dispatchers.IO) {
        val attemptedAt = System.currentTimeMillis()
        dao.markRefreshAttempt(podcast.id, attemptedAt)
        try {
            val feed = feedProvider.fetch(
                feedUrl = podcast.feedUrl,
                etag = podcast.feedEtag,
                lastModified = podcast.feedLastModified,
            )
            if (feed.notModified) {
                dao.markRefreshSuccessful(
                    podcastId = podcast.id,
                    refreshedAtMillis = System.currentTimeMillis(),
                    etag = feed.etag,
                    lastModified = feed.lastModified,
                )
            } else {
                save(feed, podcast.id)
            }
            feed
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            dao.markRefreshFailed(
                podcastId = podcast.id,
                attemptedAtMillis = attemptedAt,
                error = error.message?.take(240) ?: error::class.simpleName,
            )
            throw error
        }
    }

    suspend fun updateProgress(episodeId: Long, positionMs: Long, completed: Boolean, durationMs: Long? = null) =
        dao.updateProgress(episodeId, positionMs, completed, durationMs)

    suspend fun markPlayed(episodeId: Long) {
        val episode = dao.episode(episodeId) ?: return
        dao.updateProgress(
            episodeId = episodeId,
            positionMs = episode.durationMs ?: episode.positionMs,
            completed = true,
            durationMs = episode.durationMs,
        )
    }

    suspend fun markUnplayed(episodeId: Long) {
        val episode = dao.episode(episodeId) ?: return
        dao.updateProgress(
            episodeId = episodeId,
            positionMs = 0L,
            completed = false,
            durationMs = episode.durationMs,
        )
    }

    suspend fun markCompletedAndRemoveFromQueue(episodeId: Long) {
        val episode = dao.episode(episodeId) ?: return
        dao.markCompletedAndRemoveFromQueue(
            episodeId = episodeId,
            positionMs = episode.durationMs ?: episode.positionMs,
            durationMs = episode.durationMs,
        )
    }

    suspend fun setFavorite(episodeId: Long, favorite: Boolean) = dao.setFavorite(episodeId, favorite)

    suspend fun toggleFavorite(episodeId: Long) {
        val episode = dao.episode(episodeId) ?: return
        dao.setFavorite(episodeId, !episode.favorite)
    }

    suspend fun episode(episodeId: Long): EpisodeEntity? = dao.episode(episodeId)

    suspend fun episodeGuids(podcastId: Long): List<String> = dao.episodeGuidsForPodcast(podcastId)

    suspend fun removeEpisode(episodeId: Long) {
        downloadDao.deleteByEpisodeId(episodeId)
        dao.deleteEpisode(episodeId)
        indexDelete(listOf(episodeId))
    }

    /** Persists a preview episode without adding its podcast to subscriptions. */
    suspend fun savePreviewEpisode(episode: Episode, podcast: Podcast? = null): EpisodeEntity =
        withContext(Dispatchers.IO) {
            val entity = episode.toEntity(dao.episode(episode.id))
            val podcastEntity = podcast?.let {
                val existing = dao.podcast(it.id)
                it.toEntity(existing = existing).copy(isSubscribed = existing?.isSubscribed ?: false)
            }
            if (podcastEntity == null) {
                dao.upsertEpisodes(listOf(entity))
            } else {
                dao.upsertPodcastAndEpisodes(podcastEntity, listOf(entity))
            }
            indexUpsert(listOf(entity))
            entity
        }

    suspend fun podcast(podcastId: Long): PodcastEntity? = dao.podcast(podcastId)?.let { enrich(it) }

    suspend fun setEpisodePreferVideo(episodeId: Long, preferVideo: Boolean) =
        dao.setPreferVideo(episodeId, preferVideo)

    suspend fun setPodcastPlaybackSpeed(podcastId: Long, speed: Float?) =
        dao.setPlaybackSpeed(podcastId, speed?.coerceIn(0.5f, 3f))

    suspend fun setPodcastSkipSilence(podcastId: Long, enabled: Boolean?) {
        dao.podcast(podcastId)?.let { dao.upsertPodcast(it.copy(skipSilence = enabled)) }
    }

    suspend fun setPodcastVideoDownload(podcastId: Long, enabled: Boolean) {
        dao.podcast(podcastId)?.let { dao.upsertPodcast(it.copy(includeInVideoDownload = enabled)) }
    }

    suspend fun setAllPodcastAutoDownload(enabled: Boolean) = dao.setAllAutoDownload(enabled)

    suspend fun setAllPodcastVideoDownload(enabled: Boolean) = dao.setAllVideoDownload(enabled)

    suspend fun setAllPodcastNotifications(enabled: Boolean) = dao.setAllNotifications(enabled)

    suspend fun setPodcastAutoQueue(podcastId: Long, enabled: Boolean) {
        dao.podcast(podcastId)?.let { dao.upsertPodcast(it.copy(includeInAutoQueue = enabled)) }
    }

    suspend fun setAllPodcastAutoQueue(enabled: Boolean) = dao.setAllAutoQueue(enabled)

    suspend fun updatePodcastSettings(
        podcastId: Long,
        tags: String,
        skipStartSeconds: Int,
        skipEndSeconds: Int,
        includeInAutoDownload: Boolean,
        includeInAutoQueue: Boolean,
        includeInNotifications: Boolean,
    ) {
        val podcast = dao.podcast(podcastId) ?: return
        dao.upsertPodcast(
            podcast.copy(
                tags = tags.normalizedPodcastTags().orEmpty(),
                skipStartSeconds = skipStartSeconds.coerceAtLeast(0),
                skipEndSeconds = skipEndSeconds.coerceAtLeast(0),
                includeInAutoDownload = includeInAutoDownload,
                includeInAutoQueue = includeInAutoQueue,
                includeInNotifications = includeInNotifications,
            ),
        )
        replaceCategories(
            podcastId = podcastId,
            providerId = LOCAL_DIRECTORY_PROVIDER_ID,
            categories = tags.split(',', '#').map(String::trim).filter(String::isNotBlank)
                .distinctBy(String::lowercase).map { PodcastCategory(LOCAL_DIRECTORY_PROVIDER_ID, it) },
        )
    }

    suspend fun setLocalUri(episodeId: Long, localUri: String?) = dao.setLocalUri(episodeId, localUri)

    suspend fun setLocalVideoUri(episodeId: Long, localVideoUri: String?) =
        dao.setLocalVideoUri(episodeId, localVideoUri)

    suspend fun downloadAsset(episodeId: Long, assetType: DownloadAssetType): DownloadAssetEntity? =
        downloadDao.find(episodeId, assetType)

    suspend fun downloadAssetById(downloadId: Long): DownloadAssetEntity? = downloadDao.findByDownloadId(downloadId)

    suspend fun saveDownloadAsset(asset: DownloadAssetEntity) = downloadDao.upsert(asset)

    suspend fun removeDownloadAssets(episodeId: Long) {
        downloadDao.deleteByEpisodeId(episodeId)
        dao.setLocalUri(episodeId, null)
        dao.setLocalVideoUri(episodeId, null)
    }

    suspend fun updateDownloadAsset(
        episodeId: Long,
        assetType: DownloadAssetType,
        status: DownloadAssetStatus,
        bytesDownloaded: Long,
        totalBytes: Long?,
        errorMessage: String?,
        completedAtMillis: Long? = null,
    ) = downloadDao.updateStatus(
        episodeId = episodeId,
        assetType = assetType,
        status = status,
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        errorMessage = errorMessage,
        updatedAtMillis = System.currentTimeMillis(),
        completedAtMillis = completedAtMillis,
    )

    suspend fun markDownloadRetrying(episodeId: Long, assetType: DownloadAssetType, errorMessage: String) =
        downloadDao.markRetrying(
            episodeId = episodeId,
            assetType = assetType,
            status = DownloadAssetStatus.RETRYING,
            errorMessage = errorMessage,
            updatedAtMillis = System.currentTimeMillis(),
        )

    suspend fun resolveMediaSizes(episode: EpisodeEntity): Pair<Long?, Long?> = withContext(Dispatchers.IO) {
        val client = httpClient ?: return@withContext episode.audioSizeBytes to episode.videoSizeBytes
        val shouldCheckAudio = episode.audioSizeBytes == null || episode.audioSizeBytes <= 1024L
        val audioLookup = if (shouldCheckAudio) {
            headContentLength(client, episode.audioUrl)
        } else {
            null
        }
        val audioSize = episode.audioSizeBytes?.takeIf { it > 1024L }
            ?: (audioLookup as? MediaSizeLookup.Completed)?.bytes
        val audioChecked = episode.audioSizeChecked || audioLookup is MediaSizeLookup.Completed
        var videoSize = episode.videoSizeBytes?.takeIf { it > 1024L }
        var videoChecked = episode.videoSizeChecked
        if (episode.videoUrl.isNullOrBlank() || episode.videoUrl == episode.audioUrl) {
            if (videoSize == null && episode.videoUrl == episode.audioUrl) videoSize = audioSize
            if (episode.videoUrl == episode.audioUrl && audioLookup is MediaSizeLookup.Completed) {
                videoChecked = true
            }
        } else if (videoSize == null && !videoChecked) {
            val videoLookup = headContentLength(client, episode.videoUrl)
            videoSize = (videoLookup as? MediaSizeLookup.Completed)?.bytes
            videoChecked = videoLookup is MediaSizeLookup.Completed
        }
        if (audioSize != episode.audioSizeBytes || videoSize != episode.videoSizeBytes ||
            audioChecked != episode.audioSizeChecked || videoChecked != episode.videoSizeChecked
        ) {
            dao.updateMediaSizes(episode.id, audioSize, videoSize, audioChecked, videoChecked)
        }
        audioSize to videoSize
    }

    private fun headContentLength(client: OkHttpClient, url: String): MediaSizeLookup {
        if (!url.startsWith("http", ignoreCase = true)) return MediaSizeLookup.Unavailable
        return try {
            val headBytes = client.newCall(
                Request.Builder()
                    .url(url)
                    .header("Accept-Encoding", "identity")
                    .head()
                    .build(),
            ).execute().use { response ->
                response.header("Content-Length")
                    ?.toLongOrNull()
                    ?.takeIf { response.isSuccessful && it > MIN_PLAUSIBLE_MEDIA_BYTES }
            }
            if (headBytes != null) return MediaSizeLookup.Completed(headBytes)

            // Acast's Sphinx endpoint responds to HEAD with Content-Length: 2. A range request
            // follows its media redirect and exposes the real total in Content-Range.
            client.newCall(
                Request.Builder()
                    .url(url)
                    .header("Accept-Encoding", "identity")
                    .header("Range", "bytes=0-0")
                    .get()
                    .build(),
            ).execute().use { response ->
                if (!response.isSuccessful) return MediaSizeLookup.Unavailable
                MediaSizeLookup.Completed(
                    mediaTotalBytes(
                        contentRange = response.header("Content-Range"),
                        contentLength = response.header("Content-Length"),
                    ),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            MediaSizeLookup.Unavailable
        }
    }

    private companion object {
        const val MIN_PLAUSIBLE_MEDIA_BYTES = 1024L
        const val DOWNLOAD_ASSET_QUERY_BATCH_SIZE = 900

        /**
         * How many recent publish dates to read when inferring a feed's cadence.
         *
         * [RefreshCadence] considers at most eight gaps, and needs three episodes to have any
         * pattern at all, so twelve is comfortably more than it uses.
         */
        const val CADENCE_SAMPLE_SIZE = 12
    }

    private sealed interface MediaSizeLookup {
        data class Completed(val bytes: Long?) : MediaSizeLookup
        data object Unavailable : MediaSizeLookup
    }

    suspend fun addToQueueFromInbox(episodeId: Long) = withContext(Dispatchers.IO) {
        dao.addToQueueFromInbox(episodeId)
    }

    suspend fun undoAddToQueueFromInbox(episodeId: Long) = withContext(Dispatchers.IO) {
        dao.undoAddToQueueFromInbox(episodeId)
    }

    suspend fun dismissFromInbox(episodeId: Long) = dao.setInbox(episodeId, false)

    suspend fun clearInbox() = dao.clearInbox()

    suspend fun restoreToInbox(episodeId: Long) = dao.setInbox(episodeId, true)

    suspend fun removeFromQueue(episodeId: Long) = dao.removeFromQueue(episodeId)

    /** Undoes [removeFromQueue], putting the episode back where it was rather than at the end. */
    suspend fun restoreToQueue(episodeId: Long, position: Int) = withContext(Dispatchers.IO) {
        dao.insertIntoQueue(episodeId, position)
    }

    suspend fun clearQueue() = dao.clearQueue()

    suspend fun reorderQueue(episodeIds: List<Long>) = withContext(Dispatchers.IO) {
        dao.replaceQueue(episodeIds.mapIndexed { index, episodeId -> QueueEntity(episodeId, index) })
    }

    suspend fun remove(podcast: PodcastEntity) = withContext(Dispatchers.IO) {
        dao.removePodcastIfCurrent(podcast)
    }

    /**
     * Undoes [remove] by resubscribing without refetching the feed.
     *
     * Returns whether the podcast is subscribed afterwards. False means the row is gone entirely,
     * not that it was already subscribed: [remove] is a soft delete, so the podcast surviving it with
     * `isSubscribed = 0` is the normal case and restoring it succeeds. The only way to get false is a
     * hard delete in between, which an undo has no way to recover from.
     */
    suspend fun restoreSubscription(podcast: PodcastEntity) = withContext(Dispatchers.IO) {
        dao.markPodcastSubscribed(podcast.id)
        dao.podcast(podcast.id)?.isSubscribed ?: false
    }

    private suspend fun save(
        feed: PodcastFeed,
        podcastIdOverride: Long? = null,
        importedTags: String? = null,
        fallbackDescriptionHtml: String? = null,
        appleCategories: List<String>? = null,
        appleCategoryIds: Map<String, String>? = null,
        categoryProviderId: String = APPLE_DIRECTORY_PROVIDER_ID,
        forceSubscribe: Boolean = false,
    ) {
        val podcastId = podcastIdOverride ?: feed.podcast.id
        val existingPodcast = dao.podcast(podcastId)
        val podcast = feed.podcast.toEntity(
            idOverride = podcastId,
            existing = existingPodcast,
            importedTags = importedTags,
            fallbackDescriptionHtml = fallbackDescriptionHtml,
            appleCategories = appleCategories,
            appleCategoryIds = appleCategoryIds,
        ).copy(
            feedEtag = feed.etag ?: existingPodcast?.feedEtag,
            feedLastModified = feed.lastModified ?: existingPodcast?.feedLastModified,
            lastRefreshAttemptMillis = System.currentTimeMillis(),
            lastRefreshError = null,
            isSubscribed = if (forceSubscribe) true else existingPodcast?.isSubscribed ?: true,
            subscribedAtMillis = if (forceSubscribe && existingPodcast?.isSubscribed != true) {
                System.currentTimeMillis()
            } else {
                existingPodcast?.subscribedAtMillis ?: System.currentTimeMillis()
            },
        )
        val existingEpisodes = dao.episodesForPodcast(podcastId).associateBy(EpisodeEntity::id)
        val episodes = feed.episodes.map { episode ->
            val persistedId = episodeId(podcastId, episode.guid, episode.audioUrl)
            val existing = existingEpisodes[episode.id]
                ?: existingEpisodes[persistedId]
                ?: existingEpisodes.values.firstOrNull { it.audioUrl == episode.audioUrl }
            episode.copy(id = existing?.id ?: persistedId, podcastId = podcastId).toEntity(existing)
        }
        dao.upsertPodcastAndEpisodes(podcast, episodes)
        importedTags?.let { tags ->
            replaceCategories(
                podcastId,
                LOCAL_DIRECTORY_PROVIDER_ID,
                tags.split(',', '#').map(String::trim).filter(String::isNotBlank)
                    .distinctBy(String::lowercase).map { PodcastCategory(LOCAL_DIRECTORY_PROVIDER_ID, it) },
            )
        }
        appleCategories?.let { names ->
            val ids = appleCategoryIds.orEmpty()
            replaceCategories(
                podcastId,
                categoryProviderId,
                names.map { name -> PodcastCategory(categoryProviderId, name, ids[name]) },
            )
        }
        indexUpsert(episodes)
    }

    private suspend fun enrich(podcast: PodcastEntity): PodcastEntity =
        podcast.also { it.categories = dao.categoriesForPodcast(podcast.id) }

    private fun List<PodcastEntity>.withCategories(rows: List<PodcastCategoryRow>): List<PodcastEntity> {
        val categoriesByPodcast = rows.groupBy(PodcastCategoryRow::podcastId)
        return map { podcast ->
            podcast.also {
                it.categories = categoriesByPodcast[podcast.id].orEmpty().map { row ->
                    CategoryEntity(row.id, row.providerId, row.name, row.externalId, row.parentId)
                }
            }
        }
    }

    private suspend fun replaceCategories(podcastId: Long, providerId: String, categories: List<PodcastCategory>) {
        dao.insertProviders(
            listOf(
                DirectoryProviderEntity(LOCAL_DIRECTORY_PROVIDER_ID, "Local"),
                DirectoryProviderEntity(APPLE_DIRECTORY_PROVIDER_ID, "Apple Podcasts"),
                DirectoryProviderEntity(PODCAST_INDEX_DIRECTORY_PROVIDER_ID, "Podcast Index"),
                DirectoryProviderEntity(providerId, providerId),
            ),
        )
        dao.deletePodcastCategoriesForProvider(podcastId, providerId)
        categories.distinctBy { it.name.lowercase() to it.externalId }.forEach { category ->
            dao.insertCategory(
                CategoryEntity(
                    providerId = providerId,
                    name = category.name.trim(),
                    externalId = category.externalId,
                    parentId = category.parentId,
                ),
            )
            val persisted = dao.category(providerId, category.name.trim(), category.externalId) ?: return@forEach
            dao.insertCategoryPodcast(CategoryPodcastEntity(persisted.id, podcastId))
        }
    }
}

internal fun mediaTotalBytes(contentRange: String?, contentLength: String?): Long? {
    val rangeTotal = contentRange
        ?.substringAfterLast('/', missingDelimiterValue = "")
        ?.takeUnless { it == "*" }
        ?.toLongOrNull()
        ?.takeIf { it > 1024L }
    return rangeTotal ?: contentLength?.toLongOrNull()?.takeIf { it > 1024L }
}

internal fun newEpisodesSince(
    episodes: List<EpisodeEntity>,
    existingEpisodeIdentities: Set<Pair<String, String>>,
): List<EpisodeEntity> {
    val existingGuids = existingEpisodeIdentities.mapTo(hashSetOf()) { it.first }
    val existingAudioUrls = existingEpisodeIdentities.mapTo(hashSetOf()) { it.second }
    return episodes.filterNot { it.guid in existingGuids || it.audioUrl in existingAudioUrls }
}

private fun Podcast.toEntity(
    idOverride: Long = id,
    existing: PodcastEntity? = null,
    importedTags: String? = null,
    fallbackDescriptionHtml: String? = null,
    appleCategories: List<String>? = null,
    appleCategoryIds: Map<String, String>? = null,
) = PodcastEntity(
    id = idOverride,
    title = title,
    author = author,
    feedUrl = feedUrl,
    siteUrl = siteUrl,
    descriptionHtml = descriptionHtml?.takeIf(String::isNotBlank)
        ?: fallbackDescriptionHtml?.takeIf(String::isNotBlank)
        ?: existing?.descriptionHtml,
    artworkUrl = artworkUrl,
    explicit = explicit,
    tags = importedTags.normalizedPodcastTags() ?: existing?.tags.orEmpty(),
    appleCategories = appleCategories?.let(::encodeAppleCategories) ?: existing?.appleCategories.orEmpty(),
    appleCategoryIds = appleCategoryIds?.let(::encodeAppleCategoryIds) ?: existing?.appleCategoryIds.orEmpty(),
    skipStartSeconds = existing?.skipStartSeconds ?: 0,
    skipEndSeconds = existing?.skipEndSeconds ?: 0,
    playbackSpeed = existing?.playbackSpeed,
    includeInAutoDownload = existing?.includeInAutoDownload ?: false,
    includeInVideoDownload = existing?.includeInVideoDownload ?: true,
    includeInNotifications = existing?.includeInNotifications ?: false,
    includeInAutoQueue = existing?.includeInAutoQueue ?: false,
    // Refreshes of locally retained, unsubscribed preview podcasts must not subscribe them.
    isSubscribed = existing?.isSubscribed ?: true,
    subscribedAtMillis = existing?.subscribedAtMillis ?: System.currentTimeMillis(),
    lastRefreshMillis = System.currentTimeMillis(),
)

private fun Episode.toEntity(existing: EpisodeEntity?) = EpisodeEntity(
    id = id,
    podcastId = podcastId,
    guid = guid,
    title = title,
    descriptionHtml = descriptionHtml,
    audioUrl = audioUrl,
    mimeType = mimeType,
    artworkUrl = artworkUrl,
    publishedAtMillis = publishedAtMillis,
    durationMs = durationMs ?: existing?.durationMs,
    linkUrl = linkUrl ?: existing?.linkUrl,
    videoUrl = videoUrl ?: existing?.videoUrl,
    videoMimeType = videoMimeType ?: existing?.videoMimeType,
    preferVideo = existing?.preferVideo ?: false,
    videoPreferenceSet = existing?.videoPreferenceSet ?: false,
    audioSizeBytes = audioSizeBytes ?: existing?.audioSizeBytes?.takeIf { it > 0L },
    videoSizeBytes = videoSizeBytes ?: existing?.videoSizeBytes?.takeIf { it > 0L },
    audioSizeChecked = existing?.audioSizeChecked ?: false,
    videoSizeChecked = existing?.videoSizeChecked ?: false,
    explicit = explicit,
    favorite = existing?.favorite ?: false,
    positionMs = existing?.positionMs ?: 0L,
    completed = existing?.completed ?: false,
    localUri = existing?.localUri,
    localVideoUri = existing?.localVideoUri,
    inInbox = existing?.inInbox ?: true,
    firstSeenAtMillis = existing?.firstSeenAtMillis ?: System.currentTimeMillis(),
)
