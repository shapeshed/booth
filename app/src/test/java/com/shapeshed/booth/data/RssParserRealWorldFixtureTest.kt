package com.shapeshed.booth.data

import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RssParserRealWorldFixtureTest {
    @Test
    fun louisTherouxSnapshotPreservesEpisodeArtworkAndPodcastMetadata() = runBlocking {
        val result = parseFixture("feeds/real-world/louis-theroux-episode.xml")
        val episode = result.entries.single()

        assertEquals("The Louis Theroux Podcast", result.feed.title)
        assertEquals("Spotify Studios", episode.author)
        assertEquals(
            "https://megaphone.imgix.net/podcasts/17615982-8f29-11f1-a8e6-4bb813a6a89e/image/b4b646a860dab05bd388bbfd0fcb1f3b.jpg?ixlib=rails-4.3.1&max-w=3000&max-h=3000&fit=crop&auto=format",
            episode.imageUrl,
        )
        assertEquals("https://traffic.megaphone.fm/GLT2841384206.mp3", episode.audioUrl)
        assertEquals(4_018_000L, episode.durationMs)
        assertEquals(true, result.feed.explicit)
    }

    @Test
    fun restIsPoliticsSnapshotParsesGoalhangerMegaphoneFeatures() = runBlocking {
        val result = parseFixture("feeds/real-world/rest-is-politics-episode.xml")
        val episode = result.entries.single()

        assertEquals("The Rest Is Politics", result.feed.title)
        assertEquals("Goalhanger", result.feed.author)
        assertEquals(false, result.feed.explicit)
        assertEquals("Goalhanger", episode.author)
        assertEquals(
            "https://megaphone.imgix.net/podcasts/5c7ac01e-9c92-11f1-a03f-9b8fd9afc02d/image/e72e29aac09e8a552112ce135b502b15.jpeg?ixlib=rails-4.3.1&max-w=3000&max-h=3000&fit=crop&auto=format,compress",
            episode.imageUrl,
        )
        assertEquals("https://pdst.fm/e/traffic.megaphone.fm/GLT6202238073.mp3", episode.audioUrl)
        assertEquals(1_282_000L, episode.durationMs)
        assertTrue(episode.summaryHtml.orEmpty().contains("Steve Rotheram"))
        assertTrue(episode.publishedAtMillis != null)
    }

    @Test
    fun twitSnapshotParsesPodcastIndexAndMediaRssAroundCoreEpisodeFields() = runBlocking {
        val result = parseFixture("feeds/real-world/twit-episode.xml")
        val episode = result.entries.single()

        assertEquals("This Week in Tech (Audio)", result.feed.title)
        assertEquals("TWiT", result.feed.author)
        assertEquals(false, result.feed.explicit)
        assertEquals("TWiT 1097: Gina and the Glueballs - Is AI Moving Too Fast?", episode.title)
        assertEquals("https://twit.tv/shows/this-week-in-tech/episodes/1097", episode.webUrl)
        assertEquals("https://pdst.fm/e/pscrb.fm/twit_1097.mp3", episode.audioUrl)
        assertEquals("audio/mpeg", episode.audioMimeType)
        assertEquals(169_135_867L, episode.audioSizeBytes)
        assertEquals(10_548_000L, episode.durationMs)
        assertEquals(
            "https://elroy.twit.tv/images/episodes/1097/hero.jpg",
            episode.imageUrl,
        )
        assertTrue(episode.summaryHtml.orEmpty().contains("Mark Zuckerberg"))
    }

    @Test
    fun podcastIndexExtensionsAreIgnoredWithoutBreakingCoreEpisodeParsing() = runBlocking {
        val result = parseFixture("feeds/podcasting2/podcast-index-example.xml")
        val episode = result.entries.single()

        assertEquals("Podcasting 2.0 Namespace Example", result.feed.title)
        assertEquals("Modern podcast metadata", episode.title)
        assertEquals("https://example.com/episode-1.mp3", episode.audioUrl)
        assertNotNull(episode.url)
        assertFalse(episode.summaryHtml.orEmpty().contains("Support the show"))
    }

    @Test
    fun malformedSnapshotSkipsUnusableItemsAndKeepsNestedUnknownExtensionsIsolated() = runBlocking {
        val result = parseFixture("feeds/malformed/edge-cases.xml")

        assertEquals(2, result.entries.size)
        assertEquals("https://example.com/empty.mp3", result.entries[0].audioUrl)
        assertTrue(result.entries[0].publishedAtMillis == null)
        assertEquals("Text & entities", result.entries[1].title)
        assertTrue(result.entries[1].summaryHtml.orEmpty().contains("Summary with"))
        assertFalse(result.entries[1].summaryHtml.orEmpty().contains("ignored"))
    }

    private suspend fun parseFixture(path: String): RssParseResult {
        val body = fixtureBytes(path)
        return SaxStreamingFeedParser(firstChunkSize = 1).parse(
            ByteArrayInputStream(body),
            "https://example.com/feed.xml",
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result
    }

    private fun fixtureBytes(path: String): ByteArray =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "Missing parser fixture: $path"
        }.use { it.readBytes() }
}
