package com.shapeshed.booth.data

import androidx.room.Entity
import androidx.room.Index
import java.io.File
import kotlinx.coroutines.flow.Flow

enum class DownloadAssetType { AUDIO, VIDEO }
enum class DownloadAssetStatus { QUEUED, DOWNLOADING, RETRYING, COMPLETED, FAILED, CANCELLED }

internal fun isValidDownloadedFile(file: File, expectedBytes: Long?): Boolean =
    file.isFile && file.length() > 0L &&
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

    @androidx.room.Query("SELECT * FROM download_assets WHERE episodeId = :episodeId AND assetType = :assetType LIMIT 1")
    suspend fun find(episodeId: Long, assetType: DownloadAssetType): DownloadAssetEntity?

    @androidx.room.Query("SELECT * FROM download_assets WHERE downloadId = :downloadId LIMIT 1")
    suspend fun findByDownloadId(downloadId: Long): DownloadAssetEntity?

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: DownloadAssetEntity)

    @androidx.room.Query("DELETE FROM download_assets WHERE episodeId = :episodeId")
    suspend fun deleteByEpisodeId(episodeId: Long)

    @androidx.room.Query("UPDATE download_assets SET status = :status, bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes, errorMessage = :errorMessage, updatedAtMillis = :updatedAtMillis, completedAtMillis = :completedAtMillis WHERE episodeId = :episodeId AND assetType = :assetType")
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

    @androidx.room.Query("UPDATE download_assets SET status = :status, errorMessage = :errorMessage, retryCount = retryCount + 1, updatedAtMillis = :updatedAtMillis WHERE episodeId = :episodeId AND assetType = :assetType")
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
