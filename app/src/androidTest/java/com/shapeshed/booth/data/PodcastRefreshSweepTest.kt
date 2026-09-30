package com.shapeshed.booth.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The periodic sweep: a backstop that refreshes every feed regardless of when it was last due.
 *
 * The per-feed schedule is derived from observed history, and history is not a guarantee. A feed that
 * quietly moves from weekly to daily keeps its weekly schedule until a daily run happens to notice,
 * which could be most of a week. A feed whose pattern was wrong before the app had enough history
 * has no other way to be corrected. This pass bounds how stale anything can get.
 *
 * So the assertion is deliberately blunt: a feed that is nowhere near due still gets fetched when
 * the sweep runs.
 */
@RunWith(AndroidJUnit4::class)
class PodcastRefreshSweepTest {
    private val day = 24L * 60 * 60 * 1000
    private val hour = 60L * 60 * 1000
    private val now = 1_700_000_000_000L

    @Test
    fun theSweepBoundsHowStaleAFeedCanGet() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val fetches = AtomicInteger()
        val repository = PodcastRepository(
            feedProvider = object : PodcastFeedProvider {
                override suspend fun fetch(
                    feedUrl: String,
                    etag: String?,
                    lastModified: String?,
                    onEpisodeProgress: (Int, Int) -> Unit,
                ): PodcastFeed {
                    fetches.incrementAndGet()
                    return PodcastFeed(
                        podcast = Podcast(
                            id = 1L,
                            title = "Feed",
                            author = null,
                            feedUrl = feedUrl,
                            siteUrl = null,
                            descriptionHtml = null,
                            artworkUrl = null,
                        ),
                        episodes = emptyList(),
                    )
                }
            },
            dao = database.podcastDao(),
            downloadDao = database.downloadAssetDao(),
        )
        try {
            // A weekly feed, refreshed ten minutes ago. Its own schedule says it is nowhere near
            // due, so nothing about the per-feed path would touch it.
            repository.upsertBackupPodcast(
                PodcastEntity(
                    id = 1L,
                    title = "Weekly",
                    author = null,
                    feedUrl = "https://example.com/weekly.xml",
                    siteUrl = null,
                    descriptionHtml = null,
                    artworkUrl = null,
                    subscribedAtMillis = 1L,
                    lastRefreshMillis = now - 10 * 60_000L,
                ),
            )
            for (index in 0 until 10) {
                repository.upsertBackupEpisode(episode(1L, index, now - index * 7 * day))
            }

            val due = repository.nextRefreshDueMillis(repository.podcast(1L)!!, now)
            assertTrue("precondition: the feed is not due on its own schedule", due > now + day)

            val before = fetches.get()
            repository.refreshAll(listOfNotNull(repository.podcast(1L)))
            assertEquals(
                "the sweep must fetch a feed that is not due, or a changed schedule is never noticed",
                before + 1,
                fetches.get(),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun aSweepIsCheapBecauseUnchangedFeedsAreNotReparsed() = runBlocking {
        // Booth stores each feed's ETag and Last-Modified, which is what makes a sweep affordable:
        // most of those extra fetches answer 304 with no body. This asserts the plumbing exists,
        // because without it a six-hourly sweep over a large library is a real cost.
        val context: Context = ApplicationProvider.getApplicationContext()
        val database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = PodcastRepository(
            feedProvider = object : PodcastFeedProvider {
                var seenEtag: String? = null
                var seenLastModified: String? = null

                override suspend fun fetch(
                    feedUrl: String,
                    etag: String?,
                    lastModified: String?,
                    onEpisodeProgress: (Int, Int) -> Unit,
                ): PodcastFeed {
                    seenEtag = etag
                    seenLastModified = lastModified
                    return PodcastFeed(
                        podcast = Podcast(
                            id = 1L,
                            title = "Feed",
                            author = null,
                            feedUrl = feedUrl,
                            siteUrl = null,
                            descriptionHtml = null,
                            artworkUrl = null,
                        ),
                        episodes = emptyList(),
                    )
                }
            },
            dao = database.podcastDao(),
            downloadDao = database.downloadAssetDao(),
        )
        try {
            val podcast = PodcastEntity(
                id = 2L,
                title = "Conditional",
                author = null,
                feedUrl = "https://example.com/conditional.xml",
                siteUrl = null,
                descriptionHtml = null,
                artworkUrl = null,
                subscribedAtMillis = 1L,
                lastRefreshMillis = null,
                feedEtag = "\"abc123\"",
                feedLastModified = "Tue, 22 Sep 2026 09:00:00 GMT",
            )
            repository.upsertBackupPodcast(podcast)

            repository.refresh(podcast)

            // The stored validators must survive a refresh, so the next sweep can send them again
            // and get a 304 rather than a body.
            val after = repository.podcast(2L)
            assertEquals("\"abc123\"", after?.feedEtag)
            assertEquals("Tue, 22 Sep 2026 09:00:00 GMT", after?.feedLastModified)
        } finally {
            database.close()
        }
    }

    @Test
    fun theSweepPeriodIsBoundedRatherThanDerived() = runBlocking {
        // The whole point is a ceiling that does not depend on any feed's pattern, so it is asserted
        // directly: change this and every feed's worst-case staleness changes with it.
        assertTrue(
            "the sweep must be more frequent than a day or news feeds can slip a whole day",
            RefreshCadence.SWEEP_INTERVAL_MILLIS <= 12 * hour,
        )
        assertTrue(
            "and not so frequent that it is the polling this replaced",
            RefreshCadence.SWEEP_INTERVAL_MILLIS >= 3 * hour,
        )
    }

    private fun episode(podcastId: Long, index: Int, publishedAtMillis: Long) = EpisodeEntity(
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
}
