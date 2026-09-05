package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PodcastModelsTest {
    @Test
    fun canonicalFeedUrlProducesStablePodcastId() {
        assertEquals(podcastId("example.com/feed#top"), podcastId("https://example.com/feed"))
    }

    @Test
    fun differentEpisodeIdentityProducesDifferentIds() {
        val podcast = podcastId("https://example.com/feed")
        assertNotEquals(episodeId(podcast, "one", "https://example.com/one.mp3"), episodeId(podcast, "two", "https://example.com/two.mp3"))
    }

    @Test
    fun stableGuidKeepsEpisodeIdWhenEnclosureMoves() {
        val podcast = podcastId("https://example.com/feed")

        assertEquals(
            episodeId(podcast, "episode-guid", "https://cdn-one.example.com/episode.mp3"),
            episodeId(podcast, "episode-guid", "https://cdn-two.example.com/episode.mp3"),
        )
    }
}
