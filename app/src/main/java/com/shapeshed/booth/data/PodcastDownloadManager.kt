package com.shapeshed.booth.data

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.core.net.toUri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class PodcastDownloadManager(
    private val context: Context,
    private val repository: PodcastRepository,
) {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)
    private val enqueueMutex = Mutex()

    suspend fun enqueue(
        episode: EpisodeEntity,
        assetType: DownloadAssetType,
        url: String,
        mimeType: String?,
        expectedBytes: Long?,
    ): Boolean = enqueueMutex.withLock {
        require(url.toHttpUrlOrNull()?.scheme in setOf("http", "https")) {
            "Download URL must use HTTP or HTTPS"
        }
        require(expectedBytes == null || expectedBytes >= 0L) {
            "Expected download size cannot be negative"
        }

        val existing = repository.downloadAsset(episode.id, assetType)
        if (existing != null && existing.status in ACTIVE_STATUSES &&
            existing.status != DownloadAssetStatus.RETRYING
        ) return@withLock false
        if (existing != null) {
            val existingFile = File(existing.destinationUri.toUri().path.orEmpty())
            if (existing.status == DownloadAssetStatus.COMPLETED &&
                isValidDownloadedFile(existingFile, existing.totalBytes)
            ) {
                return@withLock false
            }
            // A retry or replacement must not leave the previous DownloadManager row
            // behind. This also removes a stale corrupt destination before reuse.
            downloadManager.remove(existing.downloadId)
            if (existing.status != DownloadAssetStatus.DOWNLOADING) {
                existingFile.delete()
            }
        }

        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) ?: File(context.filesDir, "podcasts"),
            "episodes",
        )
        require(directory.exists() || directory.mkdirs()) { "Unable to create download directory" }
        val destination = File(directory, "${episode.id}.${assetType.name.lowercase()}")
        val resolvedUrl = repository.resolveDownloadUrl(url)
        val request = DownloadManager.Request(resolvedUrl.toUri())
            .setTitle(episode.title)
            .setDescription(if (assetType == DownloadAssetType.VIDEO) "Video" else "Audio")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationUri(destination.toUri())
        mimeType?.let(request::setMimeType)
        val downloadId = downloadManager.enqueue(request)
        try {
            val now = System.currentTimeMillis()
            repository.saveDownloadAsset(
                DownloadAssetEntity(
                    episodeId = episode.id,
                    assetType = assetType,
                    downloadId = downloadId,
                    sourceUrl = url,
                    destinationUri = destination.toURI().toString(),
                    status = DownloadAssetStatus.QUEUED,
                    totalBytes = expectedBytes,
                    retryCount = existing?.retryCount ?: 0,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                ),
            )
        } catch (error: Exception) {
            downloadManager.remove(downloadId)
            throw error
        }
        DownloadProgressStore.update(
            episode.id,
            DownloadProgress(0L, expectedBytes ?: 0L, android.os.SystemClock.elapsedRealtime()),
        )
        true
    }

    suspend fun syncActiveDownloads() {
        repository.downloadAssets.first()
            .filter { it.status in ACTIVE_STATUSES || it.status == DownloadAssetStatus.COMPLETED }
            .forEach { asset ->
                val cursor = downloadManager.query(
                    DownloadManager.Query().setFilterById(asset.downloadId),
                ) ?: run {
                    markMissingDownload(asset)
                    return@forEach
                }
                cursor.use {
                    if (!it.moveToFirst()) {
                        markMissingDownload(asset)
                        return@forEach
                    }
                    val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        .takeIf { value -> value >= 0L }
                    var mappedStatus = when (status) {
                        DownloadManager.STATUS_RUNNING -> DownloadAssetStatus.DOWNLOADING
                        DownloadManager.STATUS_PENDING -> DownloadAssetStatus.QUEUED
                        DownloadManager.STATUS_PAUSED -> DownloadAssetStatus.QUEUED
                        DownloadManager.STATUS_SUCCESSFUL -> DownloadAssetStatus.COMPLETED
                        else -> DownloadAssetStatus.FAILED
                    }
                    val path = asset.destinationUri.toUri().path.orEmpty()
                    val file = File(path)
                    val expectedBytes = total ?: asset.totalBytes
                    val invalidCompletedFile = mappedStatus == DownloadAssetStatus.COMPLETED &&
                        !isValidDownloadedFile(file, expectedBytes)
                    if (invalidCompletedFile) {
                        mappedStatus = DownloadAssetStatus.FAILED
                    }
                    repository.updateDownloadAsset(
                        episodeId = asset.episodeId,
                        assetType = asset.assetType,
                        status = mappedStatus,
                        bytesDownloaded = bytes,
                        totalBytes = total,
                        errorMessage = when {
                            invalidCompletedFile -> "Download completed but the local file was invalid."
                            mappedStatus == DownloadAssetStatus.FAILED -> downloadManagerFailureMessage(
                                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                            )
                            else -> null
                        },
                        completedAtMillis = if (mappedStatus == DownloadAssetStatus.COMPLETED) {
                            System.currentTimeMillis()
                        } else {
                            null
                        },
                    )
                    if (mappedStatus == DownloadAssetStatus.COMPLETED) {
                        if (asset.assetType == DownloadAssetType.VIDEO) {
                            repository.setLocalVideoUri(asset.episodeId, asset.destinationUri)
                        } else {
                            repository.setLocalUri(asset.episodeId, asset.destinationUri)
                        }
                    } else if (mappedStatus == DownloadAssetStatus.FAILED) {
                        clearLocalUri(asset)
                        DownloadProgressStore.clear(asset.episodeId)
                    }
                    DownloadProgressStore.update(
                        asset.episodeId,
                        DownloadProgress(
                                bytesDownloaded = bytes,
                                totalBytes = total ?: 0L,
                                startedAtElapsedMs = DownloadProgressStore.progress.value[asset.episodeId]
                                    ?.startedAtElapsedMs
                                    ?: android.os.SystemClock.elapsedRealtime(),
                                completed = mappedStatus == DownloadAssetStatus.COMPLETED,
                        ),
                    )
                }
            }
    }

    private suspend fun markMissingDownload(asset: DownloadAssetEntity) {
        repository.updateDownloadAsset(
            episodeId = asset.episodeId,
            assetType = asset.assetType,
            status = DownloadAssetStatus.FAILED,
            bytesDownloaded = asset.bytesDownloaded,
            totalBytes = asset.totalBytes,
            errorMessage = "The system download no longer exists.",
            completedAtMillis = null,
        )
        clearLocalUri(asset)
        DownloadProgressStore.clear(asset.episodeId)
    }

    private suspend fun clearLocalUri(asset: DownloadAssetEntity) {
        if (asset.assetType == DownloadAssetType.VIDEO) {
            repository.setLocalVideoUri(asset.episodeId, null)
        } else {
            repository.setLocalUri(asset.episodeId, null)
        }
    }

    suspend fun removeEpisodeDownloads(episodeId: Long) {
        repository.downloadAssets.first()
            .filter { it.episodeId == episodeId }
            .forEach { downloadManager.remove(it.downloadId) }
        repository.removeDownloadAssets(episodeId)
        DownloadProgressStore.clear(episodeId)
    }

    companion object {
        val ACTIVE_STATUSES = setOf(
            DownloadAssetStatus.QUEUED,
            DownloadAssetStatus.DOWNLOADING,
            DownloadAssetStatus.RETRYING,
        )
    }
}
