package com.shapeshed.booth.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.shapeshed.booth.di.boothWorkerEntryPoint
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

internal const val PLAYED_DOWNLOAD_CLEANUP_EPISODE_ID_INPUT = "playedDownloadCleanupEpisodeId"
private const val PLAYED_DOWNLOAD_CLEANUP_DELAY_HOURS = 24L

/** Removes a downloaded episode 24 hours after it was marked played. */
class PlayedDownloadCleanupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val episodeId = inputData.getLong(PLAYED_DOWNLOAD_CLEANUP_EPISODE_ID_INPUT, 0L)
        if (episodeId <= 0L) return Result.failure()

        val entryPoint = boothWorkerEntryPoint(applicationContext)
        if (!entryPoint.settings.podcastRemovePlayedDownloads.first()) return Result.success()
        val episode = entryPoint.podcastRepository.episode(episodeId) ?: return Result.success()
        if (!shouldRemovePlayedDownload(episode)) return Result.success()

        PodcastDownloadManager(applicationContext, entryPoint.podcastRepository)
            .removeEpisodeDownloads(episodeId)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, episodeId: Long) {
            val request = OneTimeWorkRequestBuilder<PlayedDownloadCleanupWorker>()
                .setInputData(workDataOf(PLAYED_DOWNLOAD_CLEANUP_EPISODE_ID_INPUT to episodeId))
                .setInitialDelay(PLAYED_DOWNLOAD_CLEANUP_DELAY_HOURS, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                workName(episodeId),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context, episodeId: Long) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(workName(episodeId))
        }

        private fun workName(episodeId: Long) = "podcast-played-download-cleanup-$episodeId"
    }
}

internal fun shouldRemovePlayedDownload(episode: EpisodeEntity): Boolean =
    episode.completed && (episode.localUri != null || episode.localVideoUri != null)
