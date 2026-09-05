package com.shapeshed.booth.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastSubscriptionRetentionDeviceTest {
    private lateinit var database: PodcastDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun unfollowHidesPodcastButRetainsEpisodeHistoryForResubscription() = runBlocking {
        val podcast = PodcastEntity(
            id = 100L,
            title = "History podcast",
            author = "Author",
            feedUrl = "https://example.com/feed.xml",
            siteUrl = null,
            descriptionHtml = null,
            artworkUrl = null,
            includeInAutoDownload = true,
            subscribedAtMillis = 1L,
            lastRefreshMillis = 2L,
        )
        val episode = EpisodeEntity(
            id = 200L,
            podcastId = podcast.id,
            guid = "episode-1",
            title = "Remembered episode",
            descriptionHtml = null,
            audioUrl = "https://example.com/episode.mp3",
            mimeType = "audio/mpeg",
            artworkUrl = null,
            publishedAtMillis = 3L,
            durationMs = 60_000L,
            positionMs = 12_000L,
            completed = false,
            localUri = null,
            inInbox = false,
            firstSeenAtMillis = 3L,
        )
        database.podcastDao().upsertPodcastAndEpisodes(podcast, listOf(episode))

        assertTrue(database.podcastDao().removePodcastIfCurrent(podcast))
        assertTrue(database.podcastDao().podcasts().isEmpty())
        assertEquals(listOf(episode), database.podcastDao().episodesForPodcast(podcast.id))
        assertFalse(database.podcastDao().podcast(podcast.id)!!.isSubscribed)
        assertTrue(database.podcastDao().podcast(podcast.id)!!.includeInAutoDownload)
        assertEquals(12_000L, database.podcastDao().episode(episode.id)!!.positionMs)

        database.podcastDao().upsertPodcast(podcast.copy(isSubscribed = true))
        assertEquals(listOf(podcast.id), database.podcastDao().podcasts().map { it.id })
        assertEquals(12_000L, database.podcastDao().episode(episode.id)!!.positionMs)
    }
}
