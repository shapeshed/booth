package com.shapeshed.booth.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shapeshed.booth.di.BoothWorkerEntryPoint
import com.shapeshed.booth.di.boothWorkerEntryPoint
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PodcastDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId < 0L) return
        val appContext = context.applicationContext
        val entryPoint = boothWorkerEntryPoint(appContext)
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                reconcile(appContext, downloadId, entryPoint)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // This scope is a root SupervisorJob with no parent, so an escaping exception
                // reaches the default handler and kills the process. That is a disproportionate
                // outcome for a background completion broadcast, and it means one transient
                // database error can take down playback. A missed reconciliation is recoverable
                // via PodcastDownloadReconciliationWorker; a crash is not.
                Log.w(TAG, "Download reconciliation failed for id=$downloadId", error)
            } finally {
                // Wrapped because a throw from finish() on the broadcast path would also crash.
                runCatching {
                    pendingResult.finish()
                    scope.cancel()
                }
            }
        }
    }

    private suspend fun reconcile(appContext: Context, downloadId: Long, entryPoint: BoothWorkerEntryPoint) {
        val repository = entryPoint.podcastRepository
        val progressStore = entryPoint.downloadProgressStore
        val asset = repository.downloadAssetById(downloadId) ?: return
        val manager = appContext.getSystemService(DownloadManager::class.java)
        val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId)) ?: return
        cursor.use {
            if (!it.moveToFirst()) return
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).takeIf { value ->
                value >=
                    0L
            }
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val path = asset.destinationUri.toUri().path.orEmpty()
                val file = File(path)
                val expectedBytes = total ?: asset.totalBytes
                val hasExpectedSizeMismatch = expectedBytes != null &&
                    expectedBytes > 0L && file.length() != expectedBytes
                if (!isValidDownloadedFile(file, expectedBytes)) {
                    repository.updateDownloadAsset(
                        asset.episodeId,
                        asset.assetType,
                        DownloadAssetStatus.FAILED,
                        bytes,
                        total,
                        if (hasExpectedSizeMismatch) {
                            "Downloaded file size did not match the expected size."
                        } else {
                            "The downloaded file was not available."
                        },
                    )
                    clearLocalUri(repository, asset)
                    file.delete()
                    progressStore.clear(asset.episodeId)
                    return
                }
                repository.updateDownloadAsset(
                    asset.episodeId,
                    asset.assetType,
                    DownloadAssetStatus.COMPLETED,
                    file.length(),
                    total,
                    null,
                    System.currentTimeMillis(),
                )
                if (asset.assetType == DownloadAssetType.VIDEO) {
                    repository.setLocalVideoUri(asset.episodeId, asset.destinationUri)
                } else {
                    repository.setLocalUri(asset.episodeId, asset.destinationUri)
                }
                progressStore.update(
                    asset.episodeId,
                    DownloadProgress(
                        file.length(),
                        total ?: file.length(),
                        android.os.SystemClock.elapsedRealtime(),
                        true,
                    ),
                )
            } else {
                val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                File(asset.destinationUri.toUri().path.orEmpty()).delete()
                val errorMessage = downloadManagerFailureMessage(reason)
                if (shouldRetryDownload(reason, asset.retryCount)) {
                    repository.markDownloadRetrying(
                        episodeId = asset.episodeId,
                        assetType = asset.assetType,
                        errorMessage = errorMessage,
                    )
                    val retryRequest = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
                        .setInputData(workDataOf(EPISODE_ID_INPUT to asset.episodeId))
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    WorkManager.getInstance(appContext).enqueueUniqueWork(
                        "podcast-download-${asset.episodeId}",
                        ExistingWorkPolicy.REPLACE,
                        retryRequest,
                    )
                    progressStore.clear(asset.episodeId)
                    return
                }
                repository.updateDownloadAsset(
                    asset.episodeId,
                    asset.assetType,
                    DownloadAssetStatus.FAILED,
                    bytes,
                    total,
                    errorMessage,
                )
                clearLocalUri(repository, asset)
                progressStore.clear(asset.episodeId)
            }
        }
    }
}

private const val TAG = "PodcastDownloadReceiver"

/**
 * How many times a failed transfer is retried before the row is marked FAILED for good.
 *
 * Public rather than private because the failure dialog quotes this number to the user, and a
 * second hard-coded copy in the UI would be a promise the code does not keep.
 */
const val MAX_DOWNLOAD_RETRIES = 3

internal fun shouldRetryDownload(reason: Int, retryCount: Int): Boolean =
    retryCount < MAX_DOWNLOAD_RETRIES && reason in setOf(
        DownloadManager.ERROR_CANNOT_RESUME,
        DownloadManager.ERROR_HTTP_DATA_ERROR,
    )

private suspend fun clearLocalUri(repository: PodcastRepository, asset: DownloadAssetEntity) {
    if (asset.assetType == DownloadAssetType.VIDEO) {
        repository.setLocalVideoUri(asset.episodeId, null)
    } else {
        repository.setLocalUri(asset.episodeId, null)
    }
}
