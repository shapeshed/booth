package com.shapeshed.booth.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shapeshed.booth.BoothApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Reconciles DownloadManager state after process death or a missed completion broadcast. */
class PodcastDownloadReconciliationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as BoothApp
            PodcastDownloadManager(applicationContext, app.podcastRepository).syncActiveDownloads()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val StartupWorkName = "podcast-download-reconciliation-startup"
        private const val PeriodicWorkName = "podcast-download-reconciliation-periodic"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.enqueueUniqueWork(
                StartupWorkName,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PodcastDownloadReconciliationWorker>().build(),
            )
            workManager.enqueueUniquePeriodicWork(
                PeriodicWorkName,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PodcastDownloadReconciliationWorker>(
                    15,
                    TimeUnit.MINUTES,
                ).build(),
            )
        }
    }
}
