package com.shapeshed.booth.data

import androidx.room.Entity
import androidx.room.Index
import java.io.File
import kotlinx.coroutines.flow.Flow

enum class DownloadAssetType { AUDIO, VIDEO }
enum class DownloadAssetStatus { QUEUED, WAITING_FOR_WIFI, DOWNLOADING, RETRYING, COMPLETED, FAILED, CANCELLED }

internal fun isValidDownloadedFile(file: File, expectedBytes: Long?): Boolean = file.isFile && file.length() > 0L &&
    (expectedBytes == null || expectedBytes <= 0L || file.length() == expectedBytes)

@Entity(
    tableName = "download_assets",
    primaryKeys = ["episodeId", "assetType"],
    indices = [Index(value = ["downloadId"], unique = true)],
)
data class DownloadAssetEntity(
    val episodeId: Long,
    val assetType: DownloadAssetType,
    val downloadId: Long,
    val sourceUrl: String,
    val destinationUri: String,
    val status: DownloadAssetStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long? = null,
)

@androidx.room.Dao
interface DownloadAssetDao {
    @androidx.room.Query("SELECT * FROM download_assets ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<DownloadAssetEntity>>

    @androidx.room.Query(
        "SELECT * FROM download_assets WHERE episodeId = :episodeId AND assetType = :assetType LIMIT 1",
    )
    suspend fun find(episodeId: Long, assetType: DownloadAssetType): DownloadAssetEntity?

    @androidx.room.Query("SELECT * FROM download_assets WHERE downloadId = :downloadId LIMIT 1")
    suspend fun findByDownloadId(downloadId: Long): DownloadAssetEntity?

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: DownloadAssetEntity)

    @androidx.room.Query("DELETE FROM download_assets WHERE episodeId = :episodeId")
    suspend fun deleteByEpisodeId(episodeId: Long)

    @androidx.room.Query(
        "UPDATE download_assets SET status = :status, bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes, errorMessage = :errorMessage, updatedAtMillis = :updatedAtMillis, completedAtMillis = :completedAtMillis WHERE episodeId = :episodeId AND assetType = :assetType",
    )
    suspend fun updateStatus(
        episodeId: Long,
        assetType: DownloadAssetType,
        status: DownloadAssetStatus,
        bytesDownloaded: Long,
        totalBytes: Long?,
        errorMessage: String?,
        updatedAtMillis: Long,
        completedAtMillis: Long?,
    )

    @androidx.room.Query(
        "UPDATE download_assets SET status = :status, errorMessage = :errorMessage, retryCount = retryCount + 1, updatedAtMillis = :updatedAtMillis WHERE episodeId = :episodeId AND assetType = :assetType",
    )
    suspend fun markRetrying(
        episodeId: Long,
        assetType: DownloadAssetType,
        status: DownloadAssetStatus,
        errorMessage: String,
        updatedAtMillis: Long,
    )
}

class DownloadAssetConverters {
    @androidx.room.TypeConverter
    fun assetTypeToStorage(value: DownloadAssetType): String = value.name

    @androidx.room.TypeConverter
    fun storageToAssetType(value: String): DownloadAssetType = DownloadAssetType.valueOf(value)

    @androidx.room.TypeConverter
    fun statusToStorage(value: DownloadAssetStatus): String = value.name

    @androidx.room.TypeConverter
    fun storageToStatus(value: String): DownloadAssetStatus = DownloadAssetStatus.valueOf(value)
}

/**
 * Whether a local media file exists for this episode, in either form.
 *
 * This is the retention question, and it is deliberately *not* the same as what the UI shows. It
 * answers "is there a file on disk that reclaiming space could remove", so it counts audio and
 * video and ignores in-flight progress. The rules that delete downloads use it, which is why it
 * needs to be one named predicate rather than the same expression copied into each of them: a
 * divergence here would either leak files or delete one the user still needs.
 *
 * Blank counts as absent, since an empty string names no file. Nothing writes one today, but
 * retention would then skip that episode forever and leak it.
 */
internal fun EpisodeEntity.hasLocalMedia(): Boolean = !localUri.isNullOrBlank() || !localVideoUri.isNullOrBlank()

/**
 * Whether to offer the UI a downloaded affordance for this episode.
 *
 * Broader than [hasLocalMedia] on purpose: a download that has just finished has its progress
 * published before the episode row's localUri is written, so gating only on the column makes a
 * completed download look unavailable for a moment. It is also audio-only, because that is the
 * affordance being offered.
 */
internal fun EpisodeEntity.isDownloaded(progress: DownloadProgress?): Boolean =
    !localUri.isNullOrBlank() || progress?.completed == true
