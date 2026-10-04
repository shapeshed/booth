package com.shapeshed.booth.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Inbox badge's two moving parts, against the real database and the real settings store.
 *
 * `InboxBadgeTest` pins the comparison rule but cannot show that the query feeding it agrees: that
 * MAX is over the right rows, that unsubscribing removes a podcast's episodes from the reckoning, and
 * that dismissing an episode stops it counting. Those are exactly the parts that would rot silently,
 * because the badge would simply be wrong rather than broken.
 */
@RunWith(AndroidJUnit4::class)
class InboxBadgeDeviceTest {

    private lateinit var database: PodcastDatabase
    private lateinit var dao: PodcastDao
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.podcastDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun podcast(id: Long = 1L, subscribed: Boolean = true) = PodcastEntity(
        id = id,
        title = "Podcast $id",
        author = null,
        feedUrl = "https://example.com/$id.xml",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
        isSubscribed = subscribed,
        subscribedAtMillis = 0L,
        lastRefreshMillis = null,
    )

    private fun episode(id: Long, podcastId: Long = 1L, inInbox: Boolean = true, firstSeenAtMillis: Long? = 1_000L) =
        EpisodeEntity(
            id = id,
            podcastId = podcastId,
            guid = "episode-$id",
            title = "Episode $id",
            descriptionHtml = null,
            audioUrl = "https://example.com/$id.mp3",
            mimeType = "audio/mpeg",
            artworkUrl = null,
            publishedAtMillis = 1_000L,
            durationMs = null,
            positionMs = 0L,
            completed = false,
            localUri = null,
            inInbox = inInbox,
            firstSeenAtMillis = firstSeenAtMillis,
        )

    private suspend fun newest() = dao.observeNewestInboxFirstSeenAt().first()

    @Test
    fun anEmptyInboxReportsNothing() = runBlocking {
        dao.upsertPodcast(podcast())
        assertNull(newest())
    }

    @Test
    fun theNewestInboxArrivalIsReported() = runBlocking {
        dao.upsertPodcast(podcast())
        dao.upsertEpisodes(
            listOf(
                episode(1L, firstSeenAtMillis = 1_000L),
                episode(2L, firstSeenAtMillis = 3_000L),
                episode(3L, firstSeenAtMillis = 2_000L),
            ),
        )
        assertEquals(3_000L, newest())
    }

    /** An episode that is no longer in the Inbox must stop raising the badge. */
    @Test
    fun anEpisodeOutsideTheInboxDoesNotCount() = runBlocking {
        dao.upsertPodcast(podcast())
        dao.upsertEpisodes(
            listOf(
                episode(1L, firstSeenAtMillis = 1_000L),
                episode(2L, inInbox = false, firstSeenAtMillis = 9_000L),
            ),
        )
        assertEquals(1_000L, newest())
    }

    /**
     * The query joins on `isSubscribed`, so unsubscribing takes a podcast's episodes out of the badge
     * even though their own rows still say `inInbox = 1`.
     */
    @Test
    fun anUnsubscribedPodcastsEpisodesDoNotCount() = runBlocking {
        dao.upsertPodcast(podcast(id = 1L, subscribed = true))
        dao.upsertPodcast(podcast(id = 2L, subscribed = false))
        dao.upsertEpisodes(
            listOf(
                episode(1L, podcastId = 1L, firstSeenAtMillis = 1_000L),
                episode(2L, podcastId = 2L, firstSeenAtMillis = 9_000L),
            ),
        )
        assertEquals(1_000L, newest())
    }

    @Test
    fun aRowWithNoFirstSeenTimeDoesNotRaiseTheBadge() = runBlocking {
        dao.upsertPodcast(podcast())
        dao.upsertEpisodes(listOf(episode(1L, firstSeenAtMillis = null)))
        assertNull(newest())
    }

    /**
     * The end-to-end rule, using the query and the stored marker together.
     *
     * This is the behaviour the user sees: a fresh install badges, opening the Inbox clears it, and a
     * later arrival raises it again.
     */
    @Test
    fun theBadgeClearsWhenTheInboxIsViewedAndReturnsOnANewArrival() = runBlocking {
        val settings = SettingsStore(context)
        settings.setLastInboxViewedAtMillis(0L)

        dao.upsertPodcast(podcast())
        dao.upsertEpisodes(listOf(episode(1L, firstSeenAtMillis = 5_000L)))

        assertEquals("never opened, so the arrival is unseen", true, badgeShown(settings))

        // Opening the Inbox records now, which is after the episode arrived.
        settings.setLastInboxViewedAtMillis(6_000L)
        assertEquals("viewing clears it", false, badgeShown(settings))

        // A later arrival raises it again.
        dao.upsertEpisodes(listOf(episode(2L, firstSeenAtMillis = 7_000L)))
        assertEquals("a newer arrival raises it", true, badgeShown(settings))
    }

    private suspend fun badgeShown(settings: SettingsStore): Boolean = hasUnseenInboxEpisodes(
        newestInboxFirstSeenAtMillis = newest(),
        lastInboxViewedAtMillis = settings.lastInboxViewedAtMillis.first(),
    )
}
