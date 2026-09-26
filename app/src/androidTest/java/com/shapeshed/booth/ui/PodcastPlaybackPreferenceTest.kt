package com.shapeshed.booth.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shapeshed.booth.data.PodcastDatabase
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.PodcastFeed
import com.shapeshed.booth.data.PodcastFeedProvider
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.SettingsStore
import com.shapeshed.booth.data.podcastId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
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
            val viewModel = PodcastPlaybackViewModel(SettingsStore(context), repository)

            viewModel.setPodcastPlaybackSpeed(podcast.id, 1.5f)

            withTimeout(5_000L) {
                while (repository.podcast(podcast.id)?.playbackSpeed != 1.5f) delay(10L)
            }
            assertEquals(1.5f, repository.podcast(podcast.id)?.playbackSpeed)
        } finally {
            database.close()
        }
    }
}
