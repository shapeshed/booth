package com.shapeshed.booth.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shapeshed.booth.di.boothWorkerEntryPoint
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reconciles DownloadManager state after process death or a missed completion broadcast. */
class PodcastDownloadReconciliationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val entryPoint = boothWorkerEntryPoint(applicationContext)
            entryPoint.downloadManager.syncActiveDownloads()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val STARTUP_WORK_NAME = "podcast-download-reconciliation-startup"
        private const val PERIODIC_WORK_NAME = "podcast-download-reconciliation-periodic"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.enqueueUniqueWork(
                STARTUP_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PodcastDownloadReconciliationWorker>().build(),
            )
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PodcastDownloadReconciliationWorker>(
                    15,
                    TimeUnit.MINUTES,
                ).build(),
            )
        }
    }
}
