package com.shapeshed.booth.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shapeshed.booth.di.boothWorkerEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

const val EPISODE_ID_INPUT = "episodeId"

/**
 * How many times an enqueue is retried before it is treated as permanently broken.
 *
 * DownloadManager owns the transfer and does its own retrying, so this worker only ever hands the
 * request over. An enqueue that keeps throwing is therefore not going to start working: the usual
 * cause is a URL the system refuses outright, which no amount of backoff fixes. `Result.retry()`
 * with no cap would reschedule for as long as WorkManager allows, which is up to five hours of
 * backoff per attempt and forever after that.
 */
internal const val MAX_ENQUEUE_ATTEMPTS = 3

/** Whether a failed enqueue is worth another attempt, or has run out of them. */
internal enum class EnqueueFailure {
    /** Transient. WorkManager should try again. */
    RETRY,

    /** Out of attempts. Give up and let the failure reach the UI. */
    GIVE_UP,
}

/**
 * Classify a failed enqueue.
 *
 * A negative [attemptCount] is treated as out of attempts rather than as retryable, so a caller that
 * passes something nonsensical stops rather than looping.
 */
internal fun classifyEnqueueFailure(attemptCount: Int, maxAttempts: Int = MAX_ENQUEUE_ATTEMPTS): EnqueueFailure =
    if (attemptCount in 0 until maxAttempts) EnqueueFailure.RETRY else EnqueueFailure.GIVE_UP

/** Enqueues durable system downloads; DownloadManager owns the actual transfer and retries. */
class EpisodeDownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val episodeId = inputData.getLong(EPISODE_ID_INPUT, 0L)
        if (episodeId <= 0L) return@withContext Result.failure()
        val entryPoint = boothWorkerEntryPoint(applicationContext)
        val repository = entryPoint.podcastRepository
        val progressStore = entryPoint.downloadProgressStore
        val wifiOnly = entryPoint.settings.podcastDownloadNetwork.first() == PodcastDownloadNetwork.WIFI_ONLY
        val episode = repository.episode(episodeId)
            ?: return@withContext Result.failure().also { progressStore.clear(episodeId) }
        try {
            val manager = entryPoint.downloadManager
            if (episode.localUri == null) {
                manager.enqueue(
                    episode = episode,
                    assetType = DownloadAssetType.AUDIO,
                    url = episode.audioUrl,
                    mimeType = episode.mimeType,
                    expectedBytes = episode.audioSizeBytes,
                    wifiOnly = wifiOnly,
                )
            }
            val downloadVideo = entryPoint.settings.podcastDownloadVideos.first()
            val podcastAllowsVideo = repository.podcast(episode.podcastId)?.includeInVideoDownload ?: true
            if (downloadVideo && podcastAllowsVideo && !episode.videoUrl.isNullOrBlank() &&
                episode.localVideoUri == null && episode.videoUrl != episode.audioUrl &&
                !isHls(episode.videoUrl, episode.videoMimeType)
            ) {
                manager.enqueue(
                    episode = episode,
                    assetType = DownloadAssetType.VIDEO,
                    url = episode.videoUrl,
                    mimeType = episode.videoMimeType,
                    expectedBytes = episode.videoSizeBytes,
                    wifiOnly = wifiOnly,
                )
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalArgumentException) {
            // A URL the system will not accept never becomes acceptable.
            progressStore.clear(episodeId)
            Result.failure()
        } catch (_: Exception) {
            when (classifyEnqueueFailure(runAttemptCount)) {
                EnqueueFailure.RETRY -> Result.retry()

                // Clearing the progress keeps the row from sitting at a percentage that will never
                // move, now that nothing is going to drive it.
                EnqueueFailure.GIVE_UP -> {
                    progressStore.clear(episodeId)
                    Result.failure()
                }
            }
        }
    }

    private fun isHls(url: String, mimeType: String?): Boolean =
        mimeType.orEmpty().contains("mpegurl", ignoreCase = true) ||
            url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)
}
