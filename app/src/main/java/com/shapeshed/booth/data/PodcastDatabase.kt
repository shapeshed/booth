@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.shapeshed.booth.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.Insert
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
) {
    /** Normalized category rows, populated by the repository and not persisted in this entity. */
    @Ignore
    var categories: List<CategoryEntity> = emptyList()
}

@Entity(tableName = "directory_providers")
data class DirectoryProviderEntity(@PrimaryKey val id: String, val name: String)

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
data class CategoryPodcastEntity(val categoryId: Long, val podcastId: Long)

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

/** Search-only projection. It is maintained inline after feed persistence. */
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
data class QueueEntity(@PrimaryKey val episodeId: Long, val position: Int)

data class PodcastLatestEpisode(val podcastId: Long, val publishedAtMillis: Long?)

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1 ORDER BY title COLLATE NOCASE")
    fun observePodcasts(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts ORDER BY title COLLATE NOCASE")
    fun observeAllPodcasts(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts")
    suspend fun allPodcasts(): List<PodcastEntity>

    @Query(
        "SELECT c.*, cp.podcastId FROM categories c INNER JOIN categories_podcasts cp ON cp.categoryId = c.id ORDER BY c.name COLLATE NOCASE",
    )
    fun observePodcastCategories(): Flow<List<PodcastCategoryRow>>

    @Query("SELECT * FROM categories WHERE providerId = :providerId ORDER BY name COLLATE NOCASE")
    suspend fun categoriesForProvider(providerId: String): List<CategoryEntity>

    @Query(
        "SELECT c.* FROM categories c INNER JOIN categories_podcasts cp ON cp.categoryId = c.id WHERE cp.podcastId = :podcastId ORDER BY c.name COLLATE NOCASE",
    )
    suspend fun categoriesForPodcast(podcastId: Long): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProviders(providers: List<DirectoryProviderEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query(
        "SELECT * FROM categories WHERE providerId = :providerId AND name = :name AND ((externalId IS NULL AND :externalId IS NULL) OR externalId = :externalId) LIMIT 1",
    )
    suspend fun category(providerId: String, name: String, externalId: String?): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategoryPodcast(join: CategoryPodcastEntity)

    @Query(
        "DELETE FROM categories_podcasts WHERE podcastId = :podcastId AND categoryId IN (SELECT id FROM categories WHERE providerId = :providerId)",
    )
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

    @Query(
        """
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
    """,
    )
    fun observeInbox(): androidx.paging.PagingSource<Int, EpisodeEntity>

    @Query("SELECT * FROM episodes ORDER BY publishedAtMillis DESC, firstSeenAtMillis DESC")
    fun observeAllEpisodes(): androidx.paging.PagingSource<Int, EpisodeEntity>

    @Query(
        "SELECT e.* FROM episodes e INNER JOIN podcasts p ON e.podcastId = p.id WHERE p.id IN (:podcastIds) ORDER BY e.publishedAtMillis DESC, e.firstSeenAtMillis DESC",
    )
    fun observeEpisodesForPodcasts(podcastIds: List<Long>): androidx.paging.PagingSource<Int, EpisodeEntity>

    @Query(
        """
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
    """,
    )
    fun observeEpisodesMatching(query: String, limit: Int): Flow<List<PodcastEpisodeSearchResult>>

    @Query(
        """
        SELECT e.*, p.title AS podcastTitle, p.author AS podcastAuthor, p.artworkUrl AS podcastArtworkUrl
        FROM episodes e
        INNER JOIN podcasts p ON p.id = e.podcastId
        LEFT JOIN queue q ON q.episodeId = e.id
        ORDER BY CASE WHEN q.episodeId IS NULL THEN 1 ELSE 0 END,
            COALESCE(e.publishedAtMillis, e.firstSeenAtMillis) DESC,
            e.firstSeenAtMillis DESC
        LIMIT :limit
    """,
    )
    fun observeRecentEpisodesForSearch(limit: Int): Flow<List<PodcastEpisodeSearchResult>>

    @Query("SELECT * FROM episodes")
    suspend fun allEpisodesForSearchIndex(): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisodeSearch(entries: List<EpisodeSearchFtsEntity>)

    @Query("DELETE FROM episode_search_fts WHERE rowid IN (:episodeIds)")
    suspend fun deleteEpisodeSearch(episodeIds: List<Long>)

    @Query("DELETE FROM episode_search_fts")
    suspend fun clearEpisodeSearch()

    /** Repairs missing rows in SQLite without materializing the episode library in app memory. */
    @Query(
        """
        INSERT INTO episode_search_fts (rowid, title, descriptionHtml, podcastId)
        SELECT e.id, e.title, COALESCE(e.descriptionHtml, ''), CAST(e.podcastId AS TEXT)
        FROM episodes e
        WHERE NOT EXISTS (SELECT 1 FROM episode_search_fts f WHERE f.rowid = e.id)
        """,
    )
    suspend fun insertMissingEpisodeSearchEntries()

    /**
     * The newest publish dates for one feed, for deriving its refresh cadence.
     *
     * Only a handful are needed: the median gap between the last few says how often a feed
     * publishes, and older episodes describe a schedule it has moved on from. Deliberately selects
     * the timestamps rather than the episode rows. A library holds tens of thousands of episodes,
     * and loading them to work out a cadence is what makes a frequent check unaffordable.
     */
    @Query(
        "SELECT publishedAtMillis FROM episodes " +
            "WHERE podcastId = :podcastId AND publishedAtMillis IS NOT NULL " +
            "ORDER BY publishedAtMillis DESC LIMIT :limit",
    )
    suspend fun recentPublishTimes(podcastId: Long, limit: Int): List<Long>

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

    @Query(
        "UPDATE podcasts SET lastRefreshMillis = :refreshedAtMillis, lastRefreshAttemptMillis = :refreshedAtMillis, feedEtag = COALESCE(:etag, feedEtag), feedLastModified = COALESCE(:lastModified, feedLastModified), lastRefreshError = NULL WHERE id = :podcastId",
    )
    suspend fun markRefreshSuccessful(podcastId: Long, refreshedAtMillis: Long, etag: String?, lastModified: String?)

    @Query(
        "UPDATE podcasts SET lastRefreshAttemptMillis = :attemptedAtMillis, lastRefreshError = :error WHERE id = :podcastId",
    )
    suspend fun markRefreshFailed(podcastId: Long, attemptedAtMillis: Long, error: String?)

    @Query("UPDATE episodes SET preferVideo = :preferVideo, videoPreferenceSet = 1 WHERE id = :episodeId")
    suspend fun setPreferVideo(episodeId: Long, preferVideo: Boolean)

    @Query("UPDATE episodes SET favorite = :favorite WHERE id = :episodeId")
    suspend fun setFavorite(episodeId: Long, favorite: Boolean)

    @Query("UPDATE podcasts SET playbackSpeed = :speed WHERE id = :podcastId")
    suspend fun setPlaybackSpeed(podcastId: Long, speed: Float?)

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

    /**
     * Puts [episodeId] back at [position] in the queue, leaving every other episode in its existing
     * relative order.
     *
     * This is how an undone removal is persisted. The optimistic in-memory list can put the row back
     * on screen immediately, but only this survives a process restart, so undo does not quietly move
     * the episode to the end of the queue.
     *
     * Rewrites the whole queue in one transaction rather than trying to shift rows with an
     * `UPDATE ... SET position = position + 1`. That looks tidier and is wrong in two ways that only
     * showed up once it was run: it shifts the row being restored as well, so a re-insert at the
     * position it already occupied leaves a permanent hole in the numbering, and it cannot express
     * moving an episode that is still queued. Rebuilding from the read order also repairs any
     * numbering that is already inconsistent, and [reorderQueue] is the same whole-queue write.
     */
    @Transaction
    suspend fun insertIntoQueue(episodeId: Long, position: Int) {
        val others = queue().filterNot { it.episodeId == episodeId }
        val index = position.coerceIn(0, others.size)
        val rebuilt = others.take(index) +
            listOf(QueueEntity(episodeId, index)) +
            others.drop(index)
        replaceQueue(rebuilt.mapIndexed { offset, item -> item.copy(position = offset) })
    }

    @Transaction
    suspend fun addToQueueFromInbox(episodeId: Long) {
        addToQueue(QueueEntity(episodeId, nextQueuePosition()))
        setInbox(episodeId, false)
    }

    @Transaction
    suspend fun undoAddToQueueFromInbox(episodeId: Long) {
        removeFromQueue(episodeId)
        setInbox(episodeId, true)
    }

    @Query("DELETE FROM queue WHERE episodeId = :episodeId")
    suspend fun removeFromQueue(episodeId: Long)

    @Query("UPDATE episodes SET inInbox = :inInbox WHERE id = :episodeId")
    suspend fun setInbox(episodeId: Long, inInbox: Boolean)

    @Query("UPDATE episodes SET inInbox = 0 WHERE inInbox = 1")
    suspend fun clearInbox()

    @Query(
        "UPDATE episodes SET positionMs = :positionMs, completed = :completed, durationMs = COALESCE(:durationMs, durationMs) WHERE id = :episodeId",
    )
    suspend fun updateProgress(episodeId: Long, positionMs: Long, completed: Boolean, durationMs: Long?)

    @Transaction
    suspend fun markCompletedAndRemoveFromQueue(episodeId: Long, positionMs: Long, durationMs: Long?) {
        updateProgress(episodeId, positionMs, completed = true, durationMs = durationMs)
        removeFromQueue(episodeId)
    }

    // The `IS NOT` guards make these writes genuinely idempotent. Room invalidates a table via
    // per-row AFTER UPDATE triggers, so an UPDATE that matches zero rows fires no trigger and
    // emits nothing. Without the guard the once-a-second download poll rewrites every completed
    // episode row and re-emits every observed episodes Flow.
    @Query("UPDATE episodes SET localUri = :localUri WHERE id = :episodeId AND localUri IS NOT :localUri")
    suspend fun setLocalUri(episodeId: Long, localUri: String?)

    @Query(
        "UPDATE episodes SET localVideoUri = :localVideoUri WHERE id = :episodeId AND localVideoUri IS NOT :localVideoUri",
    )
    suspend fun setLocalVideoUri(episodeId: Long, localVideoUri: String?)

    @Query(
        "UPDATE episodes SET audioSizeBytes = COALESCE(:audioSizeBytes, audioSizeBytes), videoSizeBytes = COALESCE(:videoSizeBytes, videoSizeBytes), audioSizeChecked = :audioSizeChecked, videoSizeChecked = :videoSizeChecked WHERE id = :episodeId",
    )
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

    /**
     * Undoes [markPodcastUnsubscribed] without refetching the feed.
     *
     * Unfollowing is a soft delete, so the row and every episode are still there and this is all an
     * undo needs. Deliberately not [upsertPodcast] or the subscribe path: those re-parse the feed,
     * which would make undo a network operation that can fail, and would re-stamp
     * [PodcastEntity.subscribedAtMillis] and so lose the original subscribe order.
     */
    @Query("UPDATE podcasts SET isSubscribed = 1 WHERE id = :podcastId")
    suspend fun markPodcastSubscribed(podcastId: Long)
}

@androidx.room.TypeConverters(DownloadAssetConverters::class)
@Database(
    entities = [
        PodcastEntity::class,
        EpisodeEntity::class,
        EpisodeSearchFtsEntity::class,
        QueueEntity::class,
        DownloadAssetEntity::class,
        DirectoryProviderEntity::class,
        CategoryEntity::class,
        CategoryPodcastEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class PodcastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun downloadAssetDao(): DownloadAssetDao

    companion object {
        /**
         * Note for anyone upgrading from a build that predates the first release: that schema was
         * squashed from v27 to v1, so a database file from it is at a *higher* version than this code
         * declares. Room treats that as a downgrade and throws on open rather than migrating
         * backwards. Uninstall before running, which drops the old file and lets v1 be created.
         *
         * `fallbackToDestructiveMigration` would paper over that, and must not be added: it would
         * silently wipe a real library the first time a genuine migration ran. Migrations from here
         * on are real and belong in [PodcastDatabaseMigrations], each with a MigrationTestHelper
         * test.
         */
        fun create(context: Context): PodcastDatabase = Room.databaseBuilder(
            context,
            PodcastDatabase::class.java,
            "podcasts.db",
        ).addMigrations(MIGRATION_1_2).build()
    }
}
