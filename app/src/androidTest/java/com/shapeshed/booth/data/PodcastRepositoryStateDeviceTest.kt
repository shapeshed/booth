package com.shapeshed.booth.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastRepositoryStateDeviceTest {
    private lateinit var database: PodcastDatabase
    private lateinit var repository: PodcastRepository
    private lateinit var feedProvider: MutableFeedProvider
    private val podcast = Podcast(
        id = podcastId(FEED_URL),
        title = "State test podcast",
        author = "Test author",
        feedUrl = FEED_URL,
        siteUrl = null,
        descriptionHtml = "Description",
        artworkUrl = null,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java).build()
        feedProvider = MutableFeedProvider(initialFeed())
        repository = PodcastRepository(
            feedProvider = feedProvider,
            dao = database.podcastDao(),
            downloadDao = database.downloadAssetDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshMergesNewEpisodesAndPreservesExistingState() = runBlocking {
        repository.subscribe(FEED_URL)
        val first = database.podcastDao().episodesForPodcast(podcast.id).single()
        repository.updateProgress(first.id, positionMs = 12_000L, completed = false)
        database.podcastDao().setFavorite(first.id, true)
        database.podcastDao().setLocalUri(first.id, "file:///episode.mp3")

        feedProvider.feed = updatedFeedWithChangedEnclosureAndNewEpisode()
        repository.refresh(database.podcastDao().podcasts().single())

        val episodes = database.podcastDao().episodesForPodcast(podcast.id).sortedBy(EpisodeEntity::guid)
        assertEquals(listOf("episode-1", "episode-2"), episodes.map(EpisodeEntity::guid))
        val updated = episodes.first { it.guid == "episode-1" }
        assertEquals("https://cdn.example.com/episode-1-v2.mp3", updated.audioUrl)
        assertEquals(12_000L, updated.positionMs)
        assertTrue(updated.favorite)
        assertEquals("file:///episode.mp3", updated.localUri)
        assertTrue(episodes.first { it.guid == "episode-2" }.inInbox)
    }

    @Test
    fun refreshDoesNotDeleteEpisodesMissingFromTheLatestFeed() = runBlocking {
        repository.subscribe(FEED_URL)
        feedProvider.feed = PodcastFeed(podcast, emptyList())

        repository.refresh(database.podcastDao().podcasts().single())

        assertEquals(1, database.podcastDao().episodesForPodcast(podcast.id).size)
    }

    @Test
    fun unsubscribeArchivesAndResubscribeReactivatesTheSameEpisode() = runBlocking {
        repository.subscribe(FEED_URL)
        val first = database.podcastDao().episodesForPodcast(podcast.id).single()
        repository.updateProgress(first.id, positionMs = 42_000L, completed = true)
        repository.remove(database.podcastDao().podcasts().single())

        assertTrue(database.podcastDao().podcasts().isEmpty())
        assertFalse(database.podcastDao().podcast(podcast.id)!!.isSubscribed)
        assertNotNull(database.podcastDao().episode(first.id))

        feedProvider.feed = initialFeed()
        repository.subscribe(FEED_URL)

        val restoredPodcast = database.podcastDao().podcasts().single()
        val restoredEpisode = database.podcastDao().episode(first.id)
        assertEquals(podcast.id, restoredPodcast.id)
        assertTrue(restoredPodcast.isSubscribed)
        assertNotNull(restoredEpisode)
        assertEquals(42_000L, restoredEpisode!!.positionMs)
        assertTrue(restoredEpisode.completed)
    }

    @Test
    fun notModifiedRefreshUpdatesRefreshMetadataWithoutReplacingEpisodes() = runBlocking {
        repository.subscribe(FEED_URL)
        val before = database.podcastDao().episodesForPodcast(podcast.id).single()
        feedProvider.feed = PodcastFeed(
            podcast = podcast,
            episodes = emptyList(),
            etag = "etag-2",
            lastModified = "date-2",
            notModified = true,
        )

        repository.refresh(database.podcastDao().podcasts().single())

        val after = database.podcastDao().episodesForPodcast(podcast.id).single()
        val refreshedPodcast = database.podcastDao().podcast(podcast.id)!!
        assertEquals(before, after)
        assertEquals("etag-2", refreshedPodcast.feedEtag)
        assertEquals("date-2", refreshedPodcast.feedLastModified)
    }

    @Test
    fun failedSubscribeDoesNotPersistPodcastOrEpisodes() = runBlocking {
        feedProvider.failure = IllegalStateException("streaming parser failed")

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.subscribe(FEED_URL) }
        }

        assertTrue(database.podcastDao().podcasts().isEmpty())
        assertTrue(database.podcastDao().episodesForPodcast(podcast.id).isEmpty())
    }

    @Test
    fun failedRefreshPreservesEpisodesAndOnlyRecordsRefreshError() = runBlocking {
        repository.subscribe(FEED_URL)
        val beforeEpisode = database.podcastDao().episodesForPodcast(podcast.id).single()
        val beforePodcast = database.podcastDao().podcast(podcast.id)!!
        feedProvider.failure = IllegalStateException("fallback failed")

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.refresh(beforePodcast) }
        }

        val afterPodcast = database.podcastDao().podcast(podcast.id)!!
        assertEquals(beforeEpisode, database.podcastDao().episodesForPodcast(podcast.id).single())
        assertEquals(beforePodcast.title, afterPodcast.title)
        assertEquals("fallback failed", afterPodcast.lastRefreshError)
    }

    @Test
    fun completingQueuedEpisodeMarksItPlayedAndRemovesItAtomically() = runBlocking {
        repository.subscribe(FEED_URL)
        val episode = database.podcastDao().episodesForPodcast(podcast.id).single()
        repository.addToQueueFromInbox(episode.id)

        repository.markCompletedAndRemoveFromQueue(episode.id)

        val storedEpisode = database.podcastDao().episode(episode.id)!!
        assertTrue(storedEpisode.completed)
        assertEquals(episode.durationMs, storedEpisode.positionMs)
        assertTrue(database.podcastDao().queue().isEmpty())
    }

    @Test
    fun resettingEpisodeClearsPositionAndPlayedState() = runBlocking {
        repository.subscribe(FEED_URL)
        val episode = database.podcastDao().episodesForPodcast(podcast.id).single()
        repository.updateProgress(episode.id, positionMs = 42_000L, completed = true)

        repository.markUnplayed(episode.id)

        val reset = database.podcastDao().episode(episode.id)!!
        assertEquals(0L, reset.positionMs)
        assertFalse(reset.completed)
    }

    private fun initialFeed() = PodcastFeed(
        podcast = podcast,
        episodes = listOf(episode("episode-1", "https://cdn.example.com/episode-1.mp3", "First")),
    )

    private fun updatedFeedWithChangedEnclosureAndNewEpisode() = PodcastFeed(
        podcast = podcast.copy(title = "Updated title"),
        episodes = listOf(
            episode("episode-1", "https://cdn.example.com/episode-1-v2.mp3", "First updated"),
            episode("episode-2", "https://cdn.example.com/episode-2.mp3", "Second"),
        ),
    )

    private fun episode(guid: String, audioUrl: String, title: String) = Episode(
        id = episodeId(podcast.id, guid, audioUrl),
        podcastId = podcast.id,
        guid = guid,
        title = title,
        descriptionHtml = null,
        audioUrl = audioUrl,
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = 1L,
        durationMs = 60_000L,
    )

    private class MutableFeedProvider(var feed: PodcastFeed) : PodcastFeedProvider {
        var failure: Throwable? = null

        override suspend fun fetch(
            feedUrl: String,
            etag: String?,
            lastModified: String?,
            onEpisodeProgress: (processed: Int, total: Int) -> Unit,
        ): PodcastFeed {
            failure?.let { throw it }
            return feed
        }
    }

    private companion object {
        const val FEED_URL = "https://example.com/state-test.xml"
    }
}
