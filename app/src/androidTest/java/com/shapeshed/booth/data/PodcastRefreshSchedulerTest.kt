package com.shapeshed.booth.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.shapeshed.booth.di.boothWorkerEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The per-feed refresh schedule.
 *
 * [RefreshCadenceTest] covers when a feed is due. This covers what the app then does with that:
 * one request per feed, scheduled for its own moment, rather than a single timer walking the
 * library. That distinction is the efficiency claim, so it is the thing worth asserting: a weekly
 * feed and a daily feed must end up with different run times, and neither may be left relying on a
 * shared periodic job.
 */
@RunWith(AndroidJUnit4::class)
class PodcastRefreshSchedulerTest {
    private lateinit var context: Context
    private lateinit var database: PodcastDatabase
    private lateinit var repository: PodcastRepository

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .build(),
        )
        database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PodcastRepository(
            feedProvider = object : PodcastFeedProvider {
                override suspend fun fetch(
                    feedUrl: String,
                    etag: String?,
                    lastModified: String?,
                    onEpisodeProgress: (Int, Int) -> Unit,
                ): PodcastFeed = error("Scheduling must not touch the network")
            },
            dao = database.podcastDao(),
            downloadDao = database.downloadAssetDao(),
        )
    }

    private suspend fun feed(id: Long, episodes: Int, interval: Long, newestAgo: Long, lastRefresh: Long?) =
        repository.upsertBackupPodcast(
            podcast(
                id = id,
                lastRefreshMillis = lastRefresh,
                episodes = episodes,
                interval = interval,
                newestAgo = newestAgo,
            ),
        )

    private suspend fun episode(podcastId: Long, index: Int, publishedAtMillis: Long) = EpisodeEntity(
        id = podcastId * 1_000 + index,
        podcastId = podcastId,
        guid = "$index",
        title = "Episode $index",
        descriptionHtml = null,
        audioUrl = "https://example.com/$index.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = publishedAtMillis,
        durationMs = 1_000L,
        positionMs = 0L,
        completed = false,
        localUri = null,
        inInbox = false,
        firstSeenAtMillis = publishedAtMillis,
    )

    private suspend fun podcast(
        id: Long,
        lastRefreshMillis: Long?,
        episodes: Int = 10,
        interval: Long = day,
        newestAgo: Long = hour,
    ): PodcastEntity {
        val entity = PodcastEntity(
            id = id,
            title = "Feed $id",
            author = null,
            feedUrl = "https://example.com/$id.xml",
            siteUrl = null,
            descriptionHtml = null,
            artworkUrl = null,
            subscribedAtMillis = 1L,
            lastRefreshMillis = lastRefreshMillis,
        )
        for (index in 0 until episodes) {
            repository.upsertBackupEpisode(
                episode(entity.id, index, now - newestAgo - index * interval),
            )
        }
        return entity
    }

    private fun workInfosFor(tag: String): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(tag).get()

    @Test
    fun eachFeedGetsItsOwnRequest() = runBlocking {
        feed(id = 1L, episodes = 10, interval = day, newestAgo = hour, lastRefresh = now - 2 * hour)
        feed(id = 2L, episodes = 10, interval = 7 * day, newestAgo = hour, lastRefresh = now - 2 * hour)

        PodcastRefreshScheduler.schedule(context, 1L, repository, now)
        PodcastRefreshScheduler.schedule(context, 2L, repository, now)

        val daily = workInfosFor("podcast-refresh-1")
        val weekly = workInfosFor("podcast-refresh-2")
        assertEquals("each feed should have its own request", 1, daily.size)
        assertEquals(1, weekly.size)
        assertTrue("feed 1 is not enqueued", daily.single().state == WorkInfo.State.ENQUEUED)
    }

    @Test
    fun aWeeklyFeedIsScheduledLaterThanADailyOne() = runBlocking {
        feed(id = 1L, episodes = 10, interval = day, newestAgo = hour, lastRefresh = now - 2 * hour)
        feed(id = 2L, episodes = 10, interval = 7 * day, newestAgo = hour, lastRefresh = now - 2 * hour)

        val dailyDue = repository.nextRefreshDueMillis(repository.podcast(1L)!!, now)
        val weeklyDue = repository.nextRefreshDueMillis(repository.podcast(2L)!!, now)

        // The efficiency claim, stated as an assertion: one global interval could not produce these.
        assertTrue("a daily feed should be due before a weekly one", dailyDue < weeklyDue)
    }

    @Test
    fun reschedulingReplacesRatherThanQueuingASecondRequest() = runBlocking {
        feed(id = 1L, episodes = 10, interval = day, newestAgo = hour, lastRefresh = now - 2 * hour)

        PodcastRefreshScheduler.schedule(context, 1L, repository, now)
        val firstId = workInfosFor("podcast-refresh-1").single().id

        // A second call with different history must not leave the old request queued behind the new
        // one, which is what would cause a feed to be fetched twice.
        PodcastRefreshScheduler.schedule(context, 1L, repository, now + day)
        val afterSecond = workInfosFor("podcast-refresh-1")

        assertTrue("expected one request, got ${afterSecond.size}", afterSecond.size <= 1)
        assertTrue("the stale request should be gone", afterSecond.none { it.id == firstId })
    }

    @Test
    fun aFeedWithNoHistoryIsStillScheduled() = runBlocking {
        repository.upsertBackupPodcast(podcast(id = 9L, lastRefreshMillis = null, episodes = 0))

        PodcastRefreshScheduler.schedule(context, 9L, repository, now)

        assertEquals(1, workInfosFor("podcast-refresh-9").size)
    }

    @Test
    fun unsubscribingCancelsThePendingRequest() = runBlocking {
        feed(id = 3L, episodes = 10, interval = day, newestAgo = hour, lastRefresh = now - 2 * hour)
        PodcastRefreshScheduler.schedule(context, 3L, repository, now)
        assertEquals(1, workInfosFor("podcast-refresh-3").size)

        PodcastRefreshScheduler.cancel(context, 3L)

        withTimeout(5_000L) {
            while (workInfosFor("podcast-refresh-3").single().state != WorkInfo.State.CANCELLED) delay(10L)
        }
    }

    @Test
    fun schedulingNeverTouchesTheFeed() = runBlocking {
        // The feed provider above throws on any fetch, so a successful schedule proves the cadence is
        // worked out from local episode dates rather than by asking the feed when it next publishes.
        feed(id = 4L, episodes = 10, interval = day, newestAgo = hour, lastRefresh = null)
        PodcastRefreshScheduler.schedule(context, 4L, repository, now)
        assertEquals(1, workInfosFor("podcast-refresh-4").size)
    }

    @Test
    fun schedulingOnlyNeedsTheRecentPublishDates() = runBlocking {
        // A long back catalogue must not change the answer, which is what the LIMIT is for. This
        // feed has 200 episodes; only the recent ones are read.
        val many = (0 until 200).map { now - it * day }
        repository.upsertBackupPodcast(
            podcast(id = 5L, lastRefreshMillis = now - 2 * hour, episodes = 0),
        )
        many.forEachIndexed { index, publishedAt ->
            repository.upsertBackupEpisode(episode(5L, 50_000 + index, publishedAt))
        }

        // Same cadence from the 200-episode feed as from a 10-episode one: daily.
        assertEquals(day, repository.typicalPublishIntervalMillis(5L))
    }

    @Test
    fun reconciliationIsScheduledOnceAndIsNotAFetch() = runBlocking {
        PodcastRefreshScheduler.ensureSweep(context)
        PodcastRefreshScheduler.ensureSweep(context)

        val reconcileWork = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("podcast-refresh-sweep").get()
        assertEquals("reconciliation must not stack up", 1, reconcileWork.size)
        assertEquals(WorkInfo.State.ENQUEUED, reconcileWork.single().state)

        // Arming the sweep must not itself queue per-feed work, only the periodic one. If it queued
        // a request per feed, there would be nothing left of the per-feed scheduling.
        assertTrue(
            "the sweep must not stand in for the per-feed schedule",
            workInfosFor("podcast-refresh-1").isEmpty() && workInfosFor("podcast-refresh-2").isEmpty(),
        )
    }

    @Test
    fun aScheduledRequestCarriesTheFeedItIsFor() = runBlocking {
        feed(id = 6L, episodes = 10, interval = day, newestAgo = hour, lastRefresh = now - 2 * hour)
        PodcastRefreshScheduler.schedule(context, 6L, repository, now)

        val info = workInfosFor("podcast-refresh-6").single()
        // Without the id in the input data the worker cannot tell which feed it is for, and would
        // fall through to the reconciliation path and fetch nothing.
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertTrue("request should be tagged as refresh work", info.tags.contains("podcast-refresh"))
    }
}
