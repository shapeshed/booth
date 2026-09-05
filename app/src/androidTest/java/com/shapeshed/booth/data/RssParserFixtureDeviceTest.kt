package com.shapeshed.booth.data

import androidx.test.platform.app.InstrumentationRegistry
import java.io.InputStream
import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RssParserFixtureDeviceTest {
    @Test
    fun messyRssFixturePreservesUsableItemsAndResolvesRelativeUrls() = runBlocking {
        val result = parseFixture("feeds/rss-messy.xml", "https://feeds.example.test/show/feed.xml")

        assertEquals("Messy Example Podcast", result.feed.title)
        assertEquals(3, result.entries.size)
        assertEquals("https://feeds.example.test/show/episodes/one", result.entries[0].url)
        assertEquals("https://feeds.example.test/audio/one.mp3", result.entries[0].audioUrl)
        assertEquals(3_723_000L, result.entries[0].durationMs)
        assertNull(result.entries[0].publishedAtMillis)
        assertEquals(result.entries[0].id, result.entries[1].id)
        assertEquals("https://cdn.example.test/three.mp3", result.entries[2].audioUrl)
    }

    @Test
    fun atomFixtureParsesEntryAndResolvesRelativeLinks() = runBlocking {
        val result = parseFixture("feeds/atom-feed.xml", "https://example.test/feed.atom")

        assertEquals("Atom Example Podcast", result.feed.title)
        assertEquals(1, result.entries.size)
        assertEquals("https://example.test/episodes/atom-one", result.entries.single().url)
    }

    @Test
    fun rdfFixtureParsesChannelAndItem() = runBlocking {
        val result = parseFixture("feeds/rdf-feed.xml", "https://example.test/rdf")

        assertEquals("RDF Example Podcast", result.feed.title)
        assertEquals(1, result.entries.size)
        assertEquals("https://example.test/rdf/one", result.entries.single().url)
    }

    @Test
    fun streamingParserMatchesExistingParserForMessyFixture() = runBlocking {
        val body = openFixture("feeds/rss-messy.xml").bufferedReader().use { it.readText() }
        val url = "https://feeds.example.test/show/feed.xml"
        val existing = Prof18FeedParser().parse(url, body)
        val streaming = SaxStreamingFeedParser().parse(
            ByteArrayInputStream(body.toByteArray()), url,
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result

        assertEquals(existing.feed.title, streaming.feed.title)
        assertEquals(existing.entries.size, streaming.entries.size)
        existing.entries.zip(streaming.entries).forEach { (expected, actual) ->
            assertEquals(expected.title, actual.title)
            assertEquals(expected.audioUrl, actual.audioUrl)
            assertEquals(expected.publishedAtMillis, actual.publishedAtMillis)
        }
    }

    @Test
    fun streamingParserCoversLegacyRssAndExtensions() = runBlocking {
        val legacy = streamingFixture("feeds/rss-091.xml", "https://example.test/feed.xml")
        assertEquals("RSS 0.91 Example", legacy.feed.title)
        assertEquals("https://example.test/first", legacy.entries.single().url)

        val extensions = streamingFixture("feeds/podcast-extensions.xml", "https://example.test/feeds/show.xml")
        val episode = extensions.entries.single()
        assertEquals("Extension episode", episode.title)
        assertEquals("https://example.test/feeds/episode.mp3", episode.audioUrl)
        assertEquals(0L, episode.audioSizeBytes ?: 0L)
        assertEquals(754_000L, episode.durationMs)
        assertEquals("Episode Author", episode.author)
    }

    @Test
    fun streamingParserSkipsUnusableItemsAndKeepsLaterItems() = runBlocking {
        val result = streamingFixture("feeds/malformed-items.xml", "https://example.test/feed.xml")

        assertEquals(3, result.entries.size)
        assertTrue(result.entries.any { it.title == "Usable after malformed item" })
        assertEquals(result.entries[1].id, result.entries[2].id)
    }

    @Test
    fun twitSnapshotMatchesLegacyParserForStableCoreFields() = runBlocking {
        val body = openFixture("feeds/real-world/twit-episode.xml")
            .bufferedReader().use { it.readText() }
        val url = "https://feeds.twit.tv/twit.xml"
        val legacy = Prof18FeedParser().parse(url, body)
        val streaming = SaxStreamingFeedParser().parse(
            ByteArrayInputStream(body.toByteArray()), url,
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result

        assertEquals(legacy.feed.title, streaming.feed.title)
        assertEquals(legacy.entries.size, streaming.entries.size)
        legacy.entries.zip(streaming.entries).forEach { (expected, actual) ->
            assertEquals(expected.audioUrl, actual.audioUrl)
            assertEquals(expected.publishedAtMillis, actual.publishedAtMillis)
            assertEquals(expected.durationMs, actual.durationMs)
            assertEquals(expected.webUrl, actual.webUrl)
        }
    }

    @Test
    fun productionSnapshotsMatchLegacyParserForStableCoreFields() = runBlocking {
        listOf(
            "feeds/real-world/louis-theroux-episode.xml",
            "feeds/real-world/rest-is-politics-episode.xml",
            "feeds/real-world/twit-episode.xml",
        ).forEach { path ->
            val body = openFixture(path).bufferedReader().use { it.readText() }
            val url = when {
                path.contains("louis") -> "https://feeds.megaphone.fm/GLT1948741183"
                path.contains("rest-is") -> "https://feeds.megaphone.fm/GLT9190936013"
                else -> "https://feeds.twit.tv/twit.xml"
            }
            val legacy = Prof18FeedParser().parse(url, body)
            val streaming = SaxStreamingFeedParser().parse(
                ByteArrayInputStream(body.toByteArray()), url,
            ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result

            assertEquals(path, legacy.feed.title, streaming.feed.title)
            assertEquals(path, legacy.entries.size, streaming.entries.size)
            legacy.entries.zip(streaming.entries).forEach { (expected, actual) ->
                assertEquals(path, expected.audioUrl, actual.audioUrl)
                assertEquals(path, expected.publishedAtMillis, actual.publishedAtMillis)
                assertEquals(path, expected.durationMs, actual.durationMs)
                assertEquals(path, expected.webUrl, actual.webUrl)
            }
        }
    }

    private suspend fun parseFixture(path: String, feedUrl: String): RssParseResult {
        val body = openFixture(path).bufferedReader().use { it.readText() }
        return Prof18FeedParser().parse(feedUrl, body)
    }

    private suspend fun streamingFixture(path: String, feedUrl: String): RssParseResult {
        val body = openFixture(path).bufferedReader().use { it.readText() }
        val events = SaxStreamingFeedParser().parse(
            ByteArrayInputStream(body.toByteArray()), feedUrl,
        ).toList()
        events.filterIsInstance<FeedParseEvent.Failed>().firstOrNull()?.let { failure ->
            error("Streaming parser failed: ${failure.category}: ${failure.cause}")
        }
        return events.filterIsInstance<FeedParseEvent.Completed>().single().result
    }

    private fun openFixture(path: String): InputStream =
        InstrumentationRegistry.getInstrumentation().context.assets.open(path)
}
