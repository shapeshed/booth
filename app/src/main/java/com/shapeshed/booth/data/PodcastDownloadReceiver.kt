package com.shapeshed.booth.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shapeshed.booth.BoothApp
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.NetworkType
import androidx.work.workDataOf
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class PodcastDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId < 0L) return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                reconcile(context.applicationContext as BoothApp, downloadId)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    private suspend fun reconcile(app: BoothApp, downloadId: Long) {
        val asset = app.podcastRepository.downloadAssetById(downloadId) ?: return
        val manager = app.getSystemService(DownloadManager::class.java)
        val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId)) ?: return
        cursor.use {
            if (!it.moveToFirst()) return
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).takeIf { value -> value >= 0L }
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val path = asset.destinationUri.toUri().path.orEmpty()
                val file = File(path)
                val expectedBytes = total ?: asset.totalBytes
                val hasExpectedSizeMismatch = expectedBytes != null &&
                    expectedBytes > 0L && file.length() != expectedBytes
                if (!isValidDownloadedFile(file, expectedBytes)) {
                    app.podcastRepository.updateDownloadAsset(
                        asset.episodeId, asset.assetType, DownloadAssetStatus.FAILED, bytes, total,
                        if (hasExpectedSizeMismatch) {
                            "Downloaded file size did not match the expected size."
                        } else {
                            "The downloaded file was not available."
                        },
                    )
                    clearLocalUri(app, asset)
                    file.delete()
                    DownloadProgressStore.clear(asset.episodeId)
                    return
                }
                app.podcastRepository.updateDownloadAsset(
                    asset.episodeId, asset.assetType, DownloadAssetStatus.COMPLETED, file.length(), total, null,
                    System.currentTimeMillis(),
                )
                if (asset.assetType == DownloadAssetType.VIDEO) {
                    app.podcastRepository.setLocalVideoUri(asset.episodeId, asset.destinationUri)
                } else {
                    app.podcastRepository.setLocalUri(asset.episodeId, asset.destinationUri)
                }
                DownloadProgressStore.update(
                    asset.episodeId,
                    DownloadProgress(file.length(), total ?: file.length(), android.os.SystemClock.elapsedRealtime(), true),
                )
            } else {
                val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                File(asset.destinationUri.toUri().path.orEmpty()).delete()
                val errorMessage = downloadManagerFailureMessage(reason)
                if (shouldRetryDownload(reason, asset.retryCount)) {
                    app.podcastRepository.markDownloadRetrying(
                        episodeId = asset.episodeId,
                        assetType = asset.assetType,
                        errorMessage = errorMessage,
                    )
                    val retryRequest = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
                        .setInputData(workDataOf(EPISODE_ID_INPUT to asset.episodeId))
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build(),
                        )
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    WorkManager.getInstance(app).enqueueUniqueWork(
                        "podcast-download-${asset.episodeId}",
                        ExistingWorkPolicy.REPLACE,
                        retryRequest,
                    )
                    DownloadProgressStore.clear(asset.episodeId)
                    return
                }
                app.podcastRepository.updateDownloadAsset(
                    asset.episodeId, asset.assetType, DownloadAssetStatus.FAILED, bytes, total,
                    errorMessage,
                )
                clearLocalUri(app, asset)
                DownloadProgressStore.clear(asset.episodeId)
            }
        }
    }
}

private const val MaxDownloadRetries = 3

internal fun shouldRetryDownload(reason: Int, retryCount: Int): Boolean =
    retryCount < MaxDownloadRetries && reason in setOf(
    DownloadManager.ERROR_CANNOT_RESUME,
    DownloadManager.ERROR_HTTP_DATA_ERROR,
)

private suspend fun clearLocalUri(app: BoothApp, asset: DownloadAssetEntity) {
    if (asset.assetType == DownloadAssetType.VIDEO) {
        app.podcastRepository.setLocalVideoUri(asset.episodeId, null)
    } else {
        app.podcastRepository.setLocalUri(asset.episodeId, null)
    }
}
