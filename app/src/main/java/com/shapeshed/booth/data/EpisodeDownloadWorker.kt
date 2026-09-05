package com.shapeshed.booth.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shapeshed.booth.BoothApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

const val EPISODE_ID_INPUT = "episodeId"

/** Enqueues durable system downloads; DownloadManager owns the actual transfer and retries. */
class EpisodeDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val episodeId = inputData.getLong(EPISODE_ID_INPUT, 0L)
        if (episodeId <= 0L) return@withContext Result.failure()
        val app = applicationContext as BoothApp
        val episode = app.podcastRepository.episode(episodeId)
            ?: return@withContext Result.failure().also { DownloadProgressStore.clear(episodeId) }
        try {
            val manager = PodcastDownloadManager(applicationContext, app.podcastRepository)
            if (episode.localUri == null) {
                manager.enqueue(
                    episode = episode,
                    assetType = DownloadAssetType.AUDIO,
                    url = episode.audioUrl,
                    mimeType = episode.mimeType,
                    expectedBytes = episode.audioSizeBytes,
                )
            }
            val downloadVideo = app.settings.podcastDownloadVideos.first()
            val podcastAllowsVideo = app.podcastRepository.podcast(episode.podcastId)?.includeInVideoDownload ?: true
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
                )
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalArgumentException) {
            DownloadProgressStore.clear(episodeId)
            Result.failure()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun isHls(url: String, mimeType: String?): Boolean =
        mimeType.orEmpty().contains("mpegurl", ignoreCase = true) ||
            url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)
}
