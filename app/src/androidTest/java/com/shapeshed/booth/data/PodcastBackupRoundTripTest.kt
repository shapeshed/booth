package com.shapeshed.booth.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class PodcastBackupRoundTripTest {
    @Test
    fun zipExportContainsImportableBackupDocument() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDatabase = database(context)
        try {
            val source = repository(sourceDatabase)
            source.upsertBackupPodcast(podcast())
            val bytes = PodcastBackupManager(source, SettingsStore(context)).exportZip()
            val json = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                assertEquals("backup.json", zip.nextEntry.name)
                zip.readBytes().toString(Charsets.UTF_8)
            }
            assertTrue(PodcastBackupManager.isSupported(JSONObject(json)))
        } finally {
            sourceDatabase.close()
        }
    }

    @Test
    fun restoresInboxPlaybackQueueAndDownloadIntent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDatabase = database(context)
        val targetDatabase = database(context)
        try {
            val source = repository(sourceDatabase)
            val target = repository(targetDatabase)
            val podcast = podcast()
            val episode = episode(podcast.id)
            source.upsertBackupPodcast(podcast)
            source.upsertBackupEpisode(episode)
            source.reorderQueue(listOf(episode.id))
            source.saveDownloadAsset(download(episode.id))

            val json = PodcastBackupManager(source, SettingsStore(context)).export()
            val restoredDownloads = mutableListOf<Long>()
            PodcastBackupManager(
                repository = target,
                settings = SettingsStore(context),
                restoreDownload = { restoredDownloads += it.id },
            ).import(json)

            val restoredEpisode = target.episodes(podcast.id).first().single()
            assertTrue(restoredEpisode.inInbox)
            assertEquals(125_000L, restoredEpisode.positionMs)
            assertEquals(600_000L, restoredEpisode.durationMs)
            assertEquals(listOf(episode.id), target.queue.first().map(QueueEntity::episodeId))
            assertEquals(listOf(episode.id), restoredDownloads)
        } finally {
            sourceDatabase.close()
            targetDatabase.close()
        }
    }

    private fun database(context: Context) = Room.inMemoryDatabaseBuilder(
        context,
        PodcastDatabase::class.java,
    ).allowMainThreadQueries().build()

    private fun repository(database: PodcastDatabase) = PodcastRepository(
        feedProvider = object : PodcastFeedProvider {
            override suspend fun fetch(
                feedUrl: String,
                etag: String?,
                lastModified: String?,
                onEpisodeProgress: (Int, Int) -> Unit,
            ): PodcastFeed = error("Network is not used by backup tests")
        },
        dao = database.podcastDao(),
        downloadDao = database.downloadAssetDao(),
    )

    private fun podcast() = PodcastEntity(
        id = podcastId("https://example.com/feed.xml"),
        title = "Test podcast",
        author = "Author",
        feedUrl = "https://example.com/feed.xml",
        siteUrl = null,
        descriptionHtml = "Description",
        artworkUrl = null,
        subscribedAtMillis = 1L,
        lastRefreshMillis = 2L,
    )

    private fun episode(podcastId: Long) = EpisodeEntity(
        id = episodeId(podcastId, "episode-guid", "https://example.com/episode.mp3"),
        podcastId = podcastId,
        guid = "episode-guid",
        title = "Partially played episode",
        descriptionHtml = null,
        audioUrl = "https://example.com/episode.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = 3L,
        durationMs = 600_000L,
        positionMs = 125_000L,
        completed = false,
        localUri = null,
        inInbox = true,
        firstSeenAtMillis = 4L,
    )

    private fun download(episodeId: Long) = DownloadAssetEntity(
        episodeId = episodeId,
        assetType = DownloadAssetType.AUDIO,
        downloadId = 99L,
        sourceUrl = "https://example.com/episode.mp3",
        destinationUri = "file:///tmp/episode.mp3",
        status = DownloadAssetStatus.COMPLETED,
        bytesDownloaded = 10_000L,
        totalBytes = 10_000L,
        createdAtMillis = 5L,
        updatedAtMillis = 6L,
        completedAtMillis = 6L,
    )
}
