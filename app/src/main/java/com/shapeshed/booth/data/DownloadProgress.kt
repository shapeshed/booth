package com.shapeshed.booth.data

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val startedAtElapsedMs: Long,
    val completed: Boolean = false,
) {
    val fraction: Float?
        get() = totalBytes.takeIf { it > 0L }?.let { bytesDownloaded.toFloat() / it }
}

object DownloadProgressStore {
    private val _progress = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<Long, DownloadProgress>> = _progress.asStateFlow()

    fun update(episodeId: Long, value: DownloadProgress) {
        _progress.value = _progress.value + (episodeId to value)
    }

    /** Publishes the pending state before WorkManager starts the downloader. */
    fun request(episodeId: Long, totalBytes: Long = 0L) {
        val existing = _progress.value[episodeId]
        if (existing?.completed == true) return
        update(
            episodeId,
            DownloadProgress(
                bytesDownloaded = existing?.bytesDownloaded ?: 0L,
                totalBytes = maxOf(existing?.totalBytes ?: 0L, totalBytes),
                startedAtElapsedMs = existing?.startedAtElapsedMs
                    ?: android.os.SystemClock.elapsedRealtime(),
            ),
        )
    }

    fun clear(episodeId: Long) {
        _progress.value = _progress.value - episodeId
    }
}

internal fun shouldSyncDownloads(
    assets: List<DownloadAssetEntity>,
    progress: Map<Long, DownloadProgress>,
): Boolean {
    if (assets.any { it.status in PodcastDownloadManager.ACTIVE_STATUSES }) return true
    val durableEpisodeIds = assets.mapTo(mutableSetOf(), DownloadAssetEntity::episodeId)
    return progress.any { (episodeId, value) ->
        episodeId !in durableEpisodeIds && !value.completed
    }
}

/** Combines durable DownloadManager state with the in-process byte counter. */
internal fun mergeDownloadProgress(
    assets: List<DownloadAssetEntity>,
    liveProgress: Map<Long, DownloadProgress>,
    nowElapsedMs: Long = SystemClock.elapsedRealtime(),
): Map<Long, DownloadProgress> {
    val terminalEpisodeIds = assets
        .asSequence()
        .filter { it.status == DownloadAssetStatus.FAILED || it.status == DownloadAssetStatus.CANCELLED }
        .map(DownloadAssetEntity::episodeId)
        .toSet()
    val durable = assets
        .filter {
            it.status == DownloadAssetStatus.QUEUED ||
                it.status == DownloadAssetStatus.DOWNLOADING ||
                it.status == DownloadAssetStatus.RETRYING ||
                it.status == DownloadAssetStatus.COMPLETED
        }
        .groupBy { it.episodeId }
        .mapValues { (_, episodeAssets) ->
            val audio = episodeAssets.firstOrNull { it.assetType == DownloadAssetType.AUDIO }
            val asset = audio ?: episodeAssets.maxByOrNull { it.updatedAtMillis }!!
            val live = liveProgress[asset.episodeId]
            DownloadProgress(
                bytesDownloaded = maxOf(asset.bytesDownloaded, live?.bytesDownloaded ?: 0L),
                totalBytes = asset.totalBytes ?: live?.totalBytes ?: 0L,
                startedAtElapsedMs = live?.startedAtElapsedMs ?: nowElapsedMs,
                completed = asset.assetType == DownloadAssetType.AUDIO &&
                    (asset.status == DownloadAssetStatus.COMPLETED || live?.completed == true),
            )
        }
    return durable.mapValues { (episodeId, stored) ->
        val live = liveProgress[episodeId]
        stored.copy(
            bytesDownloaded = maxOf(stored.bytesDownloaded, live?.bytesDownloaded ?: 0L),
            totalBytes = maxOf(stored.totalBytes, live?.totalBytes ?: 0L),
            startedAtElapsedMs = live?.startedAtElapsedMs ?: stored.startedAtElapsedMs,
            completed = stored.completed || live?.completed == true,
        )
    } + liveProgress.filterKeys { it !in durable && it !in terminalEpisodeIds }
}
