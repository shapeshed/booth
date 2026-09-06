@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.shapeshed.booth.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val author: String?,
    val feedUrl: String,
    val siteUrl: String?,
    val descriptionHtml: String?,
    val artworkUrl: String?,
    val explicit: Boolean? = null,
    val tags: String = "",
    /** Apple discovery categories, stored separately from user-defined tags. */
    val appleCategories: String = "",
    val appleCategoryIds: String = "",
    val skipStartSeconds: Int = 0,
    val skipEndSeconds: Int = 0,
    /** Null means that playback inherits the global speed setting. */
    val playbackSpeed: Float? = null,
    /** Null means that playback inherits the global skip-silence setting. */
    val skipSilence: Boolean? = null,
    val includeInAutoRefresh: Boolean = true,
    val includeInAutoDownload: Boolean = false,
    val includeInVideoDownload: Boolean = true,
    val includeInNotifications: Boolean = false,
    val includeInAutoQueue: Boolean = false,
    @ColumnInfo(defaultValue = "1") val isSubscribed: Boolean = true,
    val subscribedAtMillis: Long,
    val lastRefreshMillis: Long?,
    val feedEtag: String? = null,
    val feedLastModified: String? = null,
    val lastRefreshAttemptMillis: Long? = null,
    val lastRefreshError: String? = null,
)

{
    /** Normalized category rows, populated by the repository and not persisted in this entity. */
    @Ignore
    var categories: List<CategoryEntity> = emptyList()
}

@Entity(tableName = "directory_providers")
data class DirectoryProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = DirectoryProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["providerId", "name"], unique = true),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerId: String,
    val name: String,
    val externalId: String? = null,
    val parentId: Long? = null,
)

@Entity(
    tableName = "categories_podcasts",
    primaryKeys = ["categoryId", "podcastId"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["podcastId"])],
)
data class CategoryPodcastEntity(
    val categoryId: Long,
    val podcastId: Long,
)

data class PodcastCategoryRow(
    val id: Long,
    val providerId: String,
    val name: String,
    val externalId: String?,
    val parentId: Long?,
    val podcastId: Long,
)

@Entity(
    tableName = "episodes",
    indices = [
        Index(
            value = ["publishedAtMillis", "firstSeenAtMillis"],
            name = "index_episodes_published_first_seen",
        ),
        Index(
            value = ["podcastId", "publishedAtMillis", "firstSeenAtMillis"],
            name = "index_episodes_podcast_published_first_seen",
        ),
        Index(
            value = ["inInbox", "publishedAtMillis", "firstSeenAtMillis"],
            name = "index_episodes_inbox_published_first_seen",
        ),
    ],
)
data class EpisodeEntity(
    @PrimaryKey val id: Long,
    val podcastId: Long,
    val guid: String,
    val title: String,
    val descriptionHtml: String?,
    val audioUrl: String,
    val mimeType: String?,
    val artworkUrl: String?,
    val publishedAtMillis: Long?,
    val durationMs: Long?,
    val positionMs: Long,
    val completed: Boolean,
    val localUri: String?,
    val localVideoUri: String? = null,
    val inInbox: Boolean,
    val firstSeenAtMillis: Long?,
    val linkUrl: String? = null,
    val videoUrl: String? = null,
    val videoMimeType: String? = null,
    val preferVideo: Boolean = false,
    val videoPreferenceSet: Boolean = false,
    val audioSizeBytes: Long? = null,
    val videoSizeBytes: Long? = null,
    val audioSizeChecked: Boolean = false,
    val videoSizeChecked: Boolean = false,
    val explicit: Boolean? = null,
    val favorite: Boolean = false,
)

/** Search-only projection. It is maintained asynchronously after feed persistence. */
@Fts4
@Entity(tableName = "episode_search_fts")
data class EpisodeSearchFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    val title: String,
    val descriptionHtml: String,
    val podcastId: String,
)

data class PodcastEpisodeSearchResult(
    @androidx.room.Embedded val episode: EpisodeEntity,
    val podcastTitle: String,
    val podcastAuthor: String?,
    val podcastArtworkUrl: String?,
)

@Entity(
    tableName = "queue",
    indices = [Index(value = ["position"], name = "index_queue_position")],
)
data class QueueEntity(
    @PrimaryKey val episodeId: Long,
    val position: Int,
)

data class PodcastLatestEpisode(
    val podcastId: Long,
    val publishedAtMillis: Long?,
)

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1 ORDER BY title COLLATE NOCASE")
    fun observePodcasts(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts ORDER BY title COLLATE NOCASE")
    fun observeAllPodcasts(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts")
    suspend fun allPodcasts(): List<PodcastEntity>

    @Query("SELECT c.*, cp.podcastId FROM categories c INNER JOIN categories_podcasts cp ON cp.categoryId = c.id ORDER BY c.name COLLATE NOCASE")
    fun observePodcastCategories(): Flow<List<PodcastCategoryRow>>

    @Query("SELECT * FROM categories WHERE providerId = :providerId ORDER BY name COLLATE NOCASE")
    suspend fun categoriesForProvider(providerId: String): List<CategoryEntity>

    @Query("SELECT c.* FROM categories c INNER JOIN categories_podcasts cp ON cp.categoryId = c.id WHERE cp.podcastId = :podcastId ORDER BY c.name COLLATE NOCASE")
    suspend fun categoriesForPodcast(podcastId: Long): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProviders(providers: List<DirectoryProviderEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM categories WHERE providerId = :providerId AND name = :name AND ((externalId IS NULL AND :externalId IS NULL) OR externalId = :externalId) LIMIT 1")
    suspend fun category(providerId: String, name: String, externalId: String?): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoryPodcast(join: CategoryPodcastEntity)

    @Query("DELETE FROM categories_podcasts WHERE podcastId = :podcastId AND categoryId IN (SELECT id FROM categories WHERE providerId = :providerId)")
    suspend fun deletePodcastCategoriesForProvider(podcastId: Long, providerId: String)

    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1 ORDER BY title COLLATE NOCASE")
    suspend fun podcasts(): List<PodcastEntity>

    @Query("SELECT podcastId, MAX(publishedAtMillis) AS publishedAtMillis FROM episodes GROUP BY podcastId")
    fun observeLatestEpisodePublishedAt(): Flow<List<PodcastLatestEpisode>>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishedAtMillis DESC")
    fun observeEpisodes(podcastId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId")
    suspend fun episodesForPodcast(podcastId: Long): List<EpisodeEntity>

    @Query("SELECT guid FROM episodes WHERE podcastId = :podcastId")
    suspend fun episodeGuidsForPodcast(podcastId: Long): List<String>

    @Query("""
        SELECT e.id, e.podcastId, e.guid, e.title,
            NULL AS descriptionHtml,
            e.audioUrl, e.mimeType, e.artworkUrl, e.publishedAtMillis, e.durationMs,
            e.positionMs, e.completed, e.localUri, e.localVideoUri, e.inInbox,
            e.firstSeenAtMillis, e.linkUrl, e.videoUrl, e.videoMimeType,
            e.preferVideo, e.videoPreferenceSet, e.audioSizeBytes, e.videoSizeBytes,
            e.audioSizeChecked, e.videoSizeChecked, e.explicit, e.favorite
        FROM episodes e
        INNER JOIN podcasts p ON p.id = e.podcastId
        WHERE e.inInbox = 1 AND p.isSubscribed = 1
        ORDER BY e.publishedAtMillis DESC, e.firstSeenAtMillis DESC
    """)
    fun observeInbox(): androidx.paging.PagingSource<Int, EpisodeEntity>

    @Query("SELECT * FROM episodes ORDER BY publishedAtMillis DESC, firstSeenAtMillis DESC")
    fun observeAllEpisodes(): androidx.paging.PagingSource<Int, EpisodeEntity>

    @Query("SELECT e.* FROM episodes e INNER JOIN podcasts p ON e.podcastId = p.id WHERE p.id IN (:podcastIds) ORDER BY e.publishedAtMillis DESC, e.firstSeenAtMillis DESC")
    fun observeEpisodesForPodcasts(podcastIds: List<Long>): androidx.paging.PagingSource<Int, EpisodeEntity>

    @Query("""
        SELECT e.*, p.title AS podcastTitle, p.author AS podcastAuthor, p.artworkUrl AS podcastArtworkUrl
        FROM episodes e
        INNER JOIN podcasts p ON p.id = e.podcastId
        INNER JOIN episode_search_fts f ON f.rowid = e.id
        LEFT JOIN queue q ON q.episodeId = e.id
        WHERE episode_search_fts MATCH :query
        ORDER BY CASE WHEN q.episodeId IS NULL THEN 1 ELSE 0 END,
            COALESCE(e.publishedAtMillis, e.firstSeenAtMillis) DESC,
            e.firstSeenAtMillis DESC
        LIMIT :limit
    """)
    fun observeEpisodesMatching(query: String, limit: Int): Flow<List<PodcastEpisodeSearchResult>>

    @Query("""
        SELECT e.*, p.title AS podcastTitle, p.author AS podcastAuthor, p.artworkUrl AS podcastArtworkUrl
        FROM episodes e
        INNER JOIN podcasts p ON p.id = e.podcastId
        LEFT JOIN queue q ON q.episodeId = e.id
        ORDER BY CASE WHEN q.episodeId IS NULL THEN 1 ELSE 0 END,
            COALESCE(e.publishedAtMillis, e.firstSeenAtMillis) DESC,
            e.firstSeenAtMillis DESC
        LIMIT :limit
    """)
    fun observeRecentEpisodesForSearch(limit: Int): Flow<List<PodcastEpisodeSearchResult>>

    @Query("SELECT * FROM episodes")
    suspend fun allEpisodesForSearchIndex(): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisodeSearch(entries: List<EpisodeSearchFtsEntity>)

    @Query("DELETE FROM episode_search_fts WHERE rowid IN (:episodeIds)")
    suspend fun deleteEpisodeSearch(episodeIds: List<Long>)

    @Query("DELETE FROM episode_search_fts")
    suspend fun clearEpisodeSearch()

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    suspend fun episode(episodeId: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    fun observeEpisode(episodeId: Long): Flow<EpisodeEntity?>

    @Query("DELETE FROM episodes WHERE id = :episodeId")
    suspend fun deleteEpisode(episodeId: Long)

    @Query("SELECT * FROM episodes WHERE id IN (:episodeIds)")
    fun observeEpisodesByIds(episodeIds: List<Long>): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl LIMIT 1")
    suspend fun podcastByFeedUrl(feedUrl: String): PodcastEntity?

    @Query("SELECT * FROM podcasts WHERE id = :podcastId LIMIT 1")
    suspend fun podcast(podcastId: Long): PodcastEntity?

    @Query("UPDATE podcasts SET lastRefreshAttemptMillis = :attemptedAtMillis WHERE id = :podcastId")
    suspend fun markRefreshAttempt(podcastId: Long, attemptedAtMillis: Long)

    @Query("UPDATE podcasts SET lastRefreshMillis = :refreshedAtMillis, lastRefreshAttemptMillis = :refreshedAtMillis, feedEtag = COALESCE(:etag, feedEtag), feedLastModified = COALESCE(:lastModified, feedLastModified), lastRefreshError = NULL WHERE id = :podcastId")
    suspend fun markRefreshSuccessful(
        podcastId: Long,
        refreshedAtMillis: Long,
        etag: String?,
        lastModified: String?,
    )

    @Query("UPDATE podcasts SET lastRefreshAttemptMillis = :attemptedAtMillis, lastRefreshError = :error WHERE id = :podcastId")
    suspend fun markRefreshFailed(podcastId: Long, attemptedAtMillis: Long, error: String?)

    @Query("UPDATE episodes SET preferVideo = :preferVideo, videoPreferenceSet = 1 WHERE id = :episodeId")
    suspend fun setPreferVideo(episodeId: Long, preferVideo: Boolean)

    @Query("UPDATE episodes SET favorite = :favorite WHERE id = :episodeId")
    suspend fun setFavorite(episodeId: Long, favorite: Boolean)

    @Query("UPDATE podcasts SET playbackSpeed = :speed WHERE id = :podcastId")
    suspend fun setPlaybackSpeed(podcastId: Long, speed: Float?)

    @Query("UPDATE podcasts SET includeInAutoRefresh = :enabled")
    suspend fun setAllAutoRefresh(enabled: Boolean)

    @Query("UPDATE podcasts SET includeInAutoDownload = :enabled")
    suspend fun setAllAutoDownload(enabled: Boolean)

    @Query("UPDATE podcasts SET includeInVideoDownload = :enabled")
    suspend fun setAllVideoDownload(enabled: Boolean)

    @Query("UPDATE podcasts SET includeInNotifications = :enabled")
    suspend fun setAllNotifications(enabled: Boolean)

    @Query("UPDATE podcasts SET includeInAutoQueue = :enabled")
    suspend fun setAllAutoQueue(enabled: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPodcast(podcast: PodcastEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Transaction
    suspend fun upsertPodcastAndEpisodes(podcast: PodcastEntity, episodes: List<EpisodeEntity>) {
        upsertPodcast(podcast)
        if (episodes.isNotEmpty()) upsertEpisodes(episodes)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(queue: List<QueueEntity>)

    @Query("DELETE FROM queue")
    suspend fun clearQueue()

    @Query("SELECT * FROM queue ORDER BY position")
    suspend fun queue(): List<QueueEntity>

    @Query("SELECT * FROM queue ORDER BY position")
    fun observeQueue(): Flow<List<QueueEntity>>

    @Transaction
    suspend fun replaceQueue(queue: List<QueueEntity>) {
        clearQueue()
        insertQueue(queue)
    }

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM queue")
    suspend fun nextQueuePosition(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToQueue(item: QueueEntity)

    @Transaction
    suspend fun addToQueueFromInbox(episodeId: Long) {
        addToQueue(QueueEntity(episodeId, nextQueuePosition()))
        setInbox(episodeId, false)
    }

    @Query("DELETE FROM queue WHERE episodeId = :episodeId")
    suspend fun removeFromQueue(episodeId: Long)


    @Query("UPDATE episodes SET inInbox = :inInbox WHERE id = :episodeId")
    suspend fun setInbox(episodeId: Long, inInbox: Boolean)

    @Query("UPDATE episodes SET inInbox = 0 WHERE inInbox = 1")
    suspend fun clearInbox()

    @Query("UPDATE episodes SET positionMs = :positionMs, completed = :completed, durationMs = COALESCE(:durationMs, durationMs) WHERE id = :episodeId")
    suspend fun updateProgress(episodeId: Long, positionMs: Long, completed: Boolean, durationMs: Long?)

    @Transaction
    suspend fun markCompletedAndRemoveFromQueue(
        episodeId: Long,
        positionMs: Long,
        durationMs: Long?,
    ) {
        updateProgress(episodeId, positionMs, completed = true, durationMs = durationMs)
        removeFromQueue(episodeId)
    }

    @Query("UPDATE episodes SET localUri = :localUri WHERE id = :episodeId")
    suspend fun setLocalUri(episodeId: Long, localUri: String?)

    @Query("UPDATE episodes SET localVideoUri = :localVideoUri WHERE id = :episodeId")
    suspend fun setLocalVideoUri(episodeId: Long, localVideoUri: String?)

    @Query("UPDATE episodes SET audioSizeBytes = COALESCE(:audioSizeBytes, audioSizeBytes), videoSizeBytes = COALESCE(:videoSizeBytes, videoSizeBytes), audioSizeChecked = :audioSizeChecked, videoSizeChecked = :videoSizeChecked WHERE id = :episodeId")
    suspend fun updateMediaSizes(
        episodeId: Long,
        audioSizeBytes: Long?,
        videoSizeBytes: Long?,
        audioSizeChecked: Boolean,
        videoSizeChecked: Boolean,
    )

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteEpisodesForPodcast(podcastId: Long)

    @Query("DELETE FROM podcasts WHERE id = :podcastId")
    suspend fun deletePodcastById(podcastId: Long)

    @Transaction
    suspend fun removePodcast(podcastId: Long) {
        deleteEpisodesForPodcast(podcastId)
        deletePodcastById(podcastId)
    }

    @Transaction
    suspend fun removePodcastIfCurrent(expected: PodcastEntity): Boolean {
        val current = podcast(expected.id)
        if (current?.feedUrl != expected.feedUrl || current.subscribedAtMillis != expected.subscribedAtMillis) {
            return false
        }
        markPodcastUnsubscribed(expected.id)
        return true
    }

    @Query("UPDATE podcasts SET isSubscribed = 0 WHERE id = :podcastId")
    suspend fun markPodcastUnsubscribed(podcastId: Long)
}

@androidx.room.TypeConverters(DownloadAssetConverters::class)
@Database(entities = [PodcastEntity::class, EpisodeEntity::class, EpisodeSearchFtsEntity::class, QueueEntity::class, DownloadAssetEntity::class, DirectoryProviderEntity::class, CategoryEntity::class, CategoryPodcastEntity::class], version = 27, exportSchema = true)
abstract class PodcastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun downloadAssetDao(): DownloadAssetDao

    companion object {
        fun create(context: Context): PodcastDatabase = Room.databaseBuilder(
            context,
            PodcastDatabase::class.java,
            "podcasts.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27).build()
    }
}

private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE episodes ADD COLUMN inInbox INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE episodes ADD COLUMN firstSeenAtMillis INTEGER")
    }
}

private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE episodes ADD COLUMN linkUrl TEXT")
    }
}

private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN skipStartSeconds INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE podcasts ADD COLUMN skipEndSeconds INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN includeInAutoRefresh INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE podcasts ADD COLUMN includeInAutoDownload INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE episodes ADD COLUMN videoUrl TEXT")
        database.execSQL("ALTER TABLE episodes ADD COLUMN videoMimeType TEXT")
    }
}

private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE episodes ADD COLUMN localVideoUri TEXT")
    }
}

private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE episodes ADD COLUMN audioSizeBytes INTEGER")
        database.execSQL("ALTER TABLE episodes ADD COLUMN videoSizeBytes INTEGER")
    }
}

private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE episodes ADD COLUMN audioSizeChecked INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE episodes ADD COLUMN videoSizeChecked INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE episodes ADD COLUMN preferVideo INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE episodes ADD COLUMN videoPreferenceSet INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS download_assets (" +
                "episodeId INTEGER NOT NULL, " +
                "assetType TEXT NOT NULL, " +
                "downloadId INTEGER NOT NULL, " +
                "sourceUrl TEXT NOT NULL, " +
                "destinationUri TEXT NOT NULL, " +
                "status TEXT NOT NULL, " +
                "bytesDownloaded INTEGER NOT NULL, " +
                "totalBytes INTEGER, " +
                "errorMessage TEXT, " +
                "retryCount INTEGER NOT NULL, " +
                "createdAtMillis INTEGER NOT NULL, " +
                "updatedAtMillis INTEGER NOT NULL, " +
                "completedAtMillis INTEGER, " +
                "PRIMARY KEY(episodeId, assetType)" +
                ")",
        )
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_download_assets_downloadId ON download_assets(downloadId)")
    }
}

private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN includeInNotifications INTEGER NOT NULL DEFAULT 1")
    }
}

private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN explicit INTEGER")
        database.execSQL("ALTER TABLE episodes ADD COLUMN explicit INTEGER")
    }
}

private val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE episodes ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_episodes_published_first_seen " +
                "ON episodes(publishedAtMillis, firstSeenAtMillis)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_episodes_podcast_published_first_seen " +
                "ON episodes(podcastId, publishedAtMillis, firstSeenAtMillis)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_episodes_inbox_published_first_seen " +
                "ON episodes(inInbox, publishedAtMillis, firstSeenAtMillis)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_queue_position ON queue(position)",
        )
    }
}

private val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN feedEtag TEXT")
        database.execSQL("ALTER TABLE podcasts ADD COLUMN feedLastModified TEXT")
        database.execSQL("ALTER TABLE podcasts ADD COLUMN lastRefreshAttemptMillis INTEGER")
        database.execSQL("ALTER TABLE podcasts ADD COLUMN lastRefreshError TEXT")
    }
}

private val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS episode_search_fts USING FTS4(" +
                "title, descriptionHtml, podcastId)",
        )
        database.execSQL(
            "INSERT INTO episode_search_fts(rowid, title, descriptionHtml, podcastId) " +
                "SELECT id, title, COALESCE(descriptionHtml, ''), CAST(podcastId AS TEXT) FROM episodes",
        )
    }
}

private val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN isSubscribed INTEGER NOT NULL DEFAULT 1")
    }
}

private val MIGRATION_20_21 = object : androidx.room.migration.Migration(20, 21) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN playbackSpeed REAL")
    }
}

private val MIGRATION_21_22 = object : androidx.room.migration.Migration(21, 22) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN includeInVideoDownload INTEGER NOT NULL DEFAULT 1")
    }
}

private val MIGRATION_22_23 = object : androidx.room.migration.Migration(22, 23) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN includeInAutoQueue INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN appleCategories TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_24_25 = object : androidx.room.migration.Migration(24, 25) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN appleCategoryIds TEXT NOT NULL DEFAULT ''")
    }
}

internal val MIGRATION_25_26 = object : androidx.room.migration.Migration(25, 26) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS directory_providers (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
        database.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, providerId TEXT NOT NULL, name TEXT NOT NULL, externalId TEXT, parentId INTEGER, FOREIGN KEY(providerId) REFERENCES directory_providers(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_providerId_name ON categories(providerId, name)")
        database.execSQL("CREATE TABLE IF NOT EXISTS categories_podcasts (categoryId INTEGER NOT NULL, podcastId INTEGER NOT NULL, PRIMARY KEY(categoryId, podcastId), FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(podcastId) REFERENCES podcasts(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_categories_podcasts_podcastId ON categories_podcasts(podcastId)")
        database.execSQL("INSERT OR IGNORE INTO directory_providers(id, name) VALUES ('local', 'Local'), ('apple', 'Apple Podcasts'), ('podcast-index', 'Podcast Index')")
    }
}

private val MIGRATION_26_27 = object : androidx.room.migration.Migration(26, 27) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE podcasts ADD COLUMN skipSilence INTEGER")
    }
}
