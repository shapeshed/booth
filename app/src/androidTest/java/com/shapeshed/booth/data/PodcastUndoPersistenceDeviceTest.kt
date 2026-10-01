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

/**
 * The persistence half of an undone swipe.
 *
 * The undo for a queue removal and the undo for an unfollow are both a database write, and both
 * promises depend on SQL rather than Kotlin: that a restored episode goes back at the position it
 * was removed from rather than at the end, and that unfollowing can be reversed without refetching
 * the feed or disturbing the episode history. Neither is observable from the UI.
 */
@RunWith(AndroidJUnit4::class)
class PodcastUndoPersistenceDeviceTest {
    private lateinit var database: PodcastDatabase
    private lateinit var dao: PodcastDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java).build()
        dao = database.podcastDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun restoringAQueueItemPutsItBackWhereItWasRemoved() = runBlocking {
        dao.replaceQueue(listOf(1L, 2L, 3L).mapIndexed { index, id -> QueueEntity(id, index) })

        dao.insertIntoQueue(episodeId = 2L, position = 1)

        // Not merely back in the list: at the same position, so an undo does not silently move the
        // episode to the end of the queue.
        assertEquals(listOf(1L, 2L, 3L), dao.queue().map { it.episodeId })
        assertEquals(listOf(0, 1, 2), dao.queue().map { it.position })
    }

    @Test
    fun undoingAnInboxAddRemovesItFromQueueAndRestoresInboxVisibilityAtomically() = runBlocking {
        val podcast = podcast(100L)
        val episode = episode(200L, podcast.id)
        dao.upsertPodcastAndEpisodes(podcast, listOf(episode))
        dao.addToQueueFromInbox(episode.id)

        assertFalse(dao.episode(episode.id)!!.inInbox)
        assertEquals(listOf(episode.id), dao.queue().map { it.episodeId })

        dao.undoAddToQueueFromInbox(episode.id)

        assertTrue(dao.episode(episode.id)!!.inInbox)
        assertTrue(dao.queue().isEmpty())
    }

    @Test
    fun restoringAQueueItemAtTheHeadShiftsEverythingDown() = runBlocking {
        dao.replaceQueue(listOf(1L, 2L, 3L).mapIndexed { index, id -> QueueEntity(id, index) })

        dao.insertIntoQueue(episodeId = 1L, position = 0)

        assertEquals(listOf(1L, 2L, 3L), dao.queue().map { it.episodeId })
        assertEquals(listOf(0, 1, 2), dao.queue().map { it.position })
    }

    @Test
    fun restoringAQueueItemAtTheEndAppendsIt() = runBlocking {
        dao.replaceQueue(listOf(1L, 2L).mapIndexed { index, id -> QueueEntity(id, index) })

        dao.insertIntoQueue(episodeId = 3L, position = 2)

        assertEquals(listOf(1L, 2L, 3L), dao.queue().map { it.episodeId })
    }

    @Test
    fun restoringIntoAnEmptyQueueIsTheOnlyPosition() = runBlocking {
        dao.insertIntoQueue(episodeId = 1L, position = 0)

        assertEquals(listOf(1L), dao.queue().map { it.episodeId })
        assertEquals(listOf(0), dao.queue().map { it.position })
    }

    @Test
    fun restoringAnIndexPastTheEndAppendsRatherThanLeavingAGap() = runBlocking {
        dao.replaceQueue(listOf(1L, 2L).mapIndexed { index, id -> QueueEntity(id, index) })

        // A stale undo can carry an index from before another episode was removed.
        dao.insertIntoQueue(episodeId = 3L, position = 99)

        assertEquals(listOf(1L, 2L, 3L), dao.queue().map { it.episodeId })
        assertEquals(listOf(0, 1, 2), dao.queue().map { it.position })
    }

    @Test
    fun restoringAnEpisodeAlreadyInTheQueueMovesItRatherThanDuplicatingIt() = runBlocking {
        dao.replaceQueue(listOf(1L, 2L, 3L).mapIndexed { index, id -> QueueEntity(id, index) })

        // A listener who undoes, removes again by another route, and undoes the older offer.
        dao.insertIntoQueue(episodeId = 1L, position = 2)

        assertEquals(listOf(2L, 3L, 1L), dao.queue().map { it.episodeId })
        assertEquals(3, dao.queue().size)
    }

    @Test
    fun restoredQueueOrderSurvivesBeingReadBack() = runBlocking {
        dao.replaceQueue(listOf(1L, 2L, 3L, 4L).mapIndexed { index, id -> QueueEntity(id, index) })
        dao.removeFromQueue(1L)

        dao.insertIntoQueue(episodeId = 1L, position = 1)

        // The row order is what the queue is played in, so it has to survive the round trip rather
        // than only being correct in memory.
        assertEquals(listOf(2L, 1L, 3L, 4L), dao.queue().map { it.episodeId })
    }

    @Test
    fun unfollowingThenResubscribingRestoresTheSubscription() = runBlocking {
        val podcast = podcast(100L)
        dao.upsertPodcastAndEpisodes(podcast, listOf(episode(200L, podcast.id)))

        assertTrue(dao.removePodcastIfCurrent(podcast))
        assertEquals(emptyList<PodcastEntity>(), dao.podcasts())

        dao.markPodcastSubscribed(podcast.id)

        assertEquals(listOf(podcast.id), dao.podcasts().map { it.id })
    }

    @Test
    fun resubscribingKeepsTheOriginalSubscribeOrder() = runBlocking {
        val podcast = podcast(100L)
        dao.upsertPodcastAndEpisodes(podcast, listOf(episode(200L, podcast.id)))
        dao.removePodcastIfCurrent(podcast)

        dao.markPodcastSubscribed(podcast.id)

        // An undo must not re-stamp this, or the podcast would jump in an A-to-Z list to the moment
        // it was swiped away rather than where the listener put it.
        assertEquals(1L, dao.podcast(podcast.id)?.subscribedAtMillis)
    }

    @Test
    fun resubscribingKeepsTheEpisodeHistory() = runBlocking {
        val podcast = podcast(100L)
        val episode = episode(200L, podcast.id).copy(positionMs = 12_000L)
        dao.upsertPodcastAndEpisodes(podcast, listOf(episode))
        dao.removePodcastIfCurrent(podcast)

        dao.markPodcastSubscribed(podcast.id)

        assertEquals(listOf(episode.id), dao.episodesForPodcast(podcast.id).map { it.id })
        assertEquals(12_000L, dao.episode(episode.id)?.positionMs)
    }

    @Test
    fun resubscribingSomethingNeverSubscribedDoesNothing() = runBlocking {
        val podcast = podcast(100L).copy(isSubscribed = false)
        dao.upsertPodcastAndEpisodes(podcast, emptyList())

        // Deliberately not guarding on isSubscribed: the undo's intent is "it is subscribed", and
        // setting the flag is how that is expressed.
        dao.markPodcastSubscribed(podcast.id)

        assertTrue(dao.podcast(podcast.id)!!.isSubscribed)
    }

    @Test
    fun unfollowThenUndoIsIdempotent() = runBlocking {
        val podcast = podcast(100L)
        dao.upsertPodcastAndEpisodes(podcast, emptyList())
        dao.removePodcastIfCurrent(podcast)

        dao.markPodcastSubscribed(podcast.id)
        dao.markPodcastSubscribed(podcast.id)

        assertEquals(listOf(podcast.id), dao.podcasts().map { it.id })
        assertTrue(dao.podcast(podcast.id)!!.isSubscribed)
    }

    private fun podcast(id: Long) = PodcastEntity(
        id = id,
        title = "Test podcast",
        author = "Test author",
        feedUrl = "https://example.com/$id/feed.xml",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
        subscribedAtMillis = 1L,
        lastRefreshMillis = null,
    )

    private fun episode(id: Long, podcastId: Long) = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        guid = "episode-$id",
        title = "Episode $id",
        descriptionHtml = null,
        audioUrl = "https://example.com/$id.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = 60_000L,
        positionMs = 0L,
        completed = false,
        localUri = null,
        inInbox = true,
        firstSeenAtMillis = null,
    )
}
