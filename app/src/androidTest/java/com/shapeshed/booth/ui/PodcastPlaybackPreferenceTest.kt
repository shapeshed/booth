package com.shapeshed.booth.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shapeshed.booth.data.PlaybackSettings
import com.shapeshed.booth.data.PodcastDatabase
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.PodcastFeed
import com.shapeshed.booth.data.PodcastFeedProvider
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.SettingsStore
import com.shapeshed.booth.data.SleepTimerStore
import com.shapeshed.booth.data.podcastId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastPlaybackPreferenceTest {
    @Test
    fun nowPlayingSpeedCanBePersistedAsThePodcastPreference() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(
            context,
            PodcastDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val repository = PodcastRepository(
                feedProvider = object : PodcastFeedProvider {
                    override suspend fun fetch(
                        feedUrl: String,
                        etag: String?,
                        lastModified: String?,
                        onEpisodeProgress: (Int, Int) -> Unit,
                    ): PodcastFeed = error("Network is not used by playback preference tests")
                },
                dao = database.podcastDao(),
                downloadDao = database.downloadAssetDao(),
            )
            val podcast = PodcastEntity(
                id = podcastId("https://example.com/feed.xml"),
                title = "Test podcast",
                author = null,
                feedUrl = "https://example.com/feed.xml",
                siteUrl = null,
                descriptionHtml = null,
                artworkUrl = null,
                subscribedAtMillis = 1L,
                lastRefreshMillis = null,
            )
            repository.upsertBackupPodcast(podcast)
            val viewModel = PodcastPlaybackViewModel(SettingsStore(context), repository, SleepTimerStore())

            viewModel.setPodcastPlaybackSpeed(podcast.id, 1.5f)

            withTimeout(5_000L) {
                while (repository.podcast(podcast.id)?.playbackSpeed != 1.5f) delay(10L)
            }
            assertEquals(1.5f, repository.podcast(podcast.id)?.playbackSpeed)
        } finally {
            database.close()
        }
    }

    @Test
    fun clearingAPodcastSpeedMakesItFollowTheGlobalAgain() = runBlocking {
        // The app-wide default is only useful if a podcast can be released from its own number. The
        // "Use global playback speed" action writes null, and if that were stored as the previous
        // value or as some concrete default instead, the podcast would stay pinned and the global
        // would appear to do nothing for exactly the feeds that had overridden it.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val repository = repository(database)
            val podcast = testPodcast()
            repository.upsertBackupPodcast(podcast)
            val settings = SettingsStore(context)
            val viewModel = PodcastPlaybackViewModel(settings, repository, SleepTimerStore())

            settings.setPodcastPlaybackSpeed(1.5f)
            viewModel.setPodcastPlaybackSpeed(podcast.id, 0.75f)
            withTimeout(5_000L) {
                while (repository.podcast(podcast.id)?.playbackSpeed != 0.75f) delay(10L)
            }
            assertEquals(0.75f, repository.podcast(podcast.id)?.playbackSpeed)

            // Null must be stored as null, not as 0.75 and not as 1f.
            viewModel.setPodcastPlaybackSpeed(podcast.id, null)
            withTimeout(5_000L) {
                while (repository.podcast(podcast.id)?.playbackSpeed != null) delay(10L)
            }
            assertNull("the override was not cleared", repository.podcast(podcast.id)?.playbackSpeed)

            // And it now resolves to whatever the global says.
            assertEquals(
                1.5f,
                PlaybackSettings.effectiveSpeed(repository.podcast(podcast.id)?.playbackSpeed, 1.5f),
                0f,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun aPodcastSpeedOutsideTheSupportedRangeIsClamped() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val repository = repository(database)
            val podcast = testPodcast()
            repository.upsertBackupPodcast(podcast)
            val viewModel = PodcastPlaybackViewModel(SettingsStore(context), repository, SleepTimerStore())

            // The slider cannot produce these, but a restored backup or a media-session command can,
            // and Media3 rejects a speed outside its supported range rather than clamping it.
            viewModel.setPodcastPlaybackSpeed(podcast.id, 12f)
            withTimeout(5_000L) {
                while (repository.podcast(podcast.id)?.playbackSpeed == null) delay(10L)
            }
            assertEquals(PlaybackSettings.MAX_SPEED, repository.podcast(podcast.id)?.playbackSpeed)
        } finally {
            database.close()
        }
    }

    private fun repository(database: PodcastDatabase) = PodcastRepository(
        feedProvider = object : PodcastFeedProvider {
            override suspend fun fetch(
                feedUrl: String,
                etag: String?,
                lastModified: String?,
                onEpisodeProgress: (Int, Int) -> Unit,
            ): PodcastFeed = error("Network is not used by playback preference tests")
        },
        dao = database.podcastDao(),
        downloadDao = database.downloadAssetDao(),
    )

    private fun testPodcast() = PodcastEntity(
        id = podcastId("https://example.com/feed.xml"),
        title = "Test podcast",
        author = null,
        feedUrl = "https://example.com/feed.xml",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
        subscribedAtMillis = 1L,
        lastRefreshMillis = null,
    )
}
