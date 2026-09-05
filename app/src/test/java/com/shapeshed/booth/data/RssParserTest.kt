package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

class RssParserTest {
    @Test
    fun streamingParserEmitsTenThenTwentyFiveAndPreservesNamespaces() = runBlocking {
        val items = (1..36).joinToString("") { index ->
            """<item><guid>episode-$index</guid><title>Episode $index</title><dc:creator xmlns:dc="http://purl.org/dc/elements/1.1/">Author</dc:creator><enclosure url="/audio/$index.mp3" type="audio/mpeg" length="0"/></item>"""
        }
        val xml = """<?xml version="1.0"?><rss xmlns:dc="http://purl.org/dc/elements/1.1/"><channel><title>Example</title><link>https://example.com/site</link>$items</channel></rss>"""

        val events = SaxStreamingFeedParser().parse(
            ByteArrayInputStream(xml.toByteArray()), "https://example.com/feed.xml",
        ).toList()

        assertEquals("Example", (events.first { it is FeedParseEvent.FeedMetadataAvailable } as FeedParseEvent.FeedMetadataAvailable).feed.title)
        assertEquals(listOf(10, 25, 1), events.filterIsInstance<FeedParseEvent.EpisodeChunkAvailable>().map { it.entries.size })
        assertEquals(36, (events.last { it is FeedParseEvent.Completed } as FeedParseEvent.Completed).result.entries.size)
    }

    @Test
    fun streamingParserReadsNestedRssImageUrlWithoutAppendingImageLink() = runBlocking {
        val imageUrl = "https://assets.pippa.io/shows/example/show-cover.jpg"
        val xml = """
            <rss version="2.0">
              <channel>
                <title>Following On</title>
                <link>https://talksport.com/podcasts/following-on</link>
                <image>
                  <url>$imageUrl</url>
                  <link>https://talksport.com/podcasts/following-on</link>
                  <title>Following On</title>
                </image>
              </channel>
            </rss>
        """.trimIndent()

        val result = SaxStreamingFeedParser().parse(
            ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)),
            "https://feeds.example.com/following-on.xml",
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result

        assertEquals(imageUrl, result.feed.imageUrl)
        assertEquals("https://talksport.com/podcasts/following-on", result.feed.siteUrl)
    }

    @Test
    fun streamingParserReportsMalformedXmlWithoutPersistingAResult() = runBlocking {
        val events = SaxStreamingFeedParser().parse(
            ByteArrayInputStream("<rss><channel><title>Broken".toByteArray()), "https://example.com/feed.xml",
        ).toList()

        assertEquals(FailureCategory.STRUCTURAL, (events.last() as FeedParseEvent.Failed).category)
        assertEquals(null, events.filterIsInstance<FeedParseEvent.Completed>().firstOrNull())
    }

    @Test
    fun streamingParserHandlesAtomLinksRdfAndPodcastExtensionsByNamespace() = runBlocking {
        val atom = """<a:feed xmlns:a="http://www.w3.org/2005/Atom" xmlns:p="http://www.itunes.com/dtds/podcast-1.0.dtd"><a:title>Atom show</a:title><a:link href="https://example.com/site"/><a:entry><a:id>one</a:id><a:title><![CDATA[One & only]]></a:title><a:updated>2024-01-02T03:04:05Z</a:updated><a:link rel="enclosure" href="/one.mp3" type="audio/mpeg"/><p:duration>01:02:03</p:duration></a:entry></a:feed>"""
        val events = SaxStreamingFeedParser(firstChunkSize = 1).parse(
            ByteArrayInputStream(atom.toByteArray()), "https://example.com/feed.xml",
        ).toList()
        val entry = (events.last { it is FeedParseEvent.Completed } as FeedParseEvent.Completed).result.entries.single()

        assertEquals("One & only", entry.title)
        assertEquals("https://example.com/one.mp3", entry.audioUrl)
        assertEquals("https://example.com/one.mp3", entry.url)
        assertEquals(3_723_000L, entry.durationMs)
        assertEquals(1_704_164_645_000L, entry.publishedAtMillis)
    }

    @Test
    fun streamingParserPreservesSelfClosingItunesEpisodeArtwork() = runBlocking {
        val imageUrl = "https://cdn.example.com/episode.jpg"
        val xml = """
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Louis Theroux Example</title>
                <item>
                  <guid>episode-1</guid>
                  <title>Episode one</title>
                  <itunes:image href="$imageUrl"/>
                  <enclosure url="https://cdn.example.com/episode.mp3" type="audio/mpeg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val entry = SaxStreamingFeedParser(firstChunkSize = 1).parse(
            ByteArrayInputStream(xml.toByteArray()),
            "https://example.com/feed.xml",
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result.entries.single()

        assertEquals(imageUrl, entry.imageUrl)
    }

    @Test
    fun streamingParserResolvesScopedXmlBaseForEpisodeUris() = runBlocking {
        val xml = """
            <rss version="2.0" xml:base="https://example.com/root/">
              <channel xml:base="shows/">
                <title>Scoped URLs</title>
                <link>site</link>
                <item xml:base="../episodes/">
                  <guid>episode-1</guid>
                  <title>Episode one</title>
                  <itunes:image xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" href="images/one.jpg"/>
                  <enclosure url="audio/one.mp3" type="audio/mpeg"/>
                  <link>one</link>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val result = SaxStreamingFeedParser(firstChunkSize = 1).parse(
            ByteArrayInputStream(xml.toByteArray()),
            "https://example.com/feed.xml",
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result

        assertEquals("https://example.com/root/shows/site", result.feed.siteUrl)
        assertEquals("https://example.com/root/episodes/one", result.entries.single().webUrl)
        assertEquals("https://example.com/root/episodes/audio/one.mp3", result.entries.single().audioUrl)
        assertEquals("https://example.com/root/episodes/images/one.jpg", result.entries.single().imageUrl)
    }

    @Test
    fun streamingParserIgnoresUnknownNamespaceFieldsWithoutContaminatingText() = runBlocking {
        val xml = """
            <rss version="2.0" xmlns:other="urn:other">
              <channel>
                <title>Good feed title</title>
                <other:title>Wrong feed title</other:title>
                <item>
                  <guid>episode-1</guid>
                  <title>Good episode title</title>
                  <description>Summary <other:title>ignored extension</other:title> remains</description>
                  <other:title>Wrong episode title</other:title>
                  <enclosure url="https://example.com/one.mp3" type="audio/mpeg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val entry = SaxStreamingFeedParser(firstChunkSize = 1).parse(
            ByteArrayInputStream(xml.toByteArray()),
            "https://example.com/feed.xml",
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result.entries.single()

        assertEquals("Good episode title", entry.title)
        assertEquals("Summary ignored extension remains", entry.summaryHtml)
    }

    @Test
    fun streamingParserMapsPodcastExtensionsAndDoesNotTreatAudioMediaAsArtwork() = runBlocking {
        val xml = """
            <rss version="2.0"
                xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
                xmlns:media="http://search.yahoo.com/mrss/">
              <channel>
                <title>Podcast extensions</title>
                <item>
                  <guid>episode-1</guid>
                  <itunes:title>Extension title</itunes:title>
                  <itunes:author>Extension author</itunes:author>
                  <itunes:summary>Extension summary</itunes:summary>
                  <itunes:duration>12:34</itunes:duration>
                  <itunes:explicit>clean</itunes:explicit>
                  <media:content url="https://cdn.example.com/episode.m4a" type="audio/mp4"/>
                  <media:thumbnail url="https://cdn.example.com/episode.jpg"/>
                  <enclosure url="https://cdn.example.com/episode.mp3" type="audio/mpeg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val entry = SaxStreamingFeedParser(firstChunkSize = 1).parse(
            ByteArrayInputStream(xml.toByteArray()),
            "https://example.com/feed.xml",
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result.entries.single()

        assertEquals("Extension title", entry.title)
        assertEquals("Extension author", entry.author)
        assertEquals("Extension summary", entry.summaryHtml)
        assertEquals(754_000L, entry.durationMs)
        assertEquals(false, entry.explicit)
        assertEquals("https://cdn.example.com/episode.jpg", entry.imageUrl)
        assertEquals("https://cdn.example.com/episode.mp3", entry.audioUrl)
    }

    @Test
    fun streamingParserRejectsExternalDoctypeWithoutAccessingIt() = runBlocking {
        val xml = """
            <!DOCTYPE rss [<!ENTITY secret SYSTEM "file:///data/local/tmp/secret.txt">]>
            <rss version="2.0"><channel><title>&secret;</title></channel></rss>
        """.trimIndent()

        val events = SaxStreamingFeedParser().parse(
            ByteArrayInputStream(xml.toByteArray()),
            "https://example.com/feed.xml",
        ).toList()

        assertEquals(FailureCategory.STRUCTURAL, (events.last() as FeedParseEvent.Failed).category)
        assertTrue(events.none { it is FeedParseEvent.Completed })
    }

    @Test
    fun streamingParserSelectsRdfItemDialect() = runBlocking {
        val rdf = """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"><channel><title>RDF show</title><link>https://example.com</link></channel><item><guid>https://example.com/one</guid><title>One</title><enclosure url="https://example.com/one.mp3" type="audio/mpeg"/></item></rdf:RDF>"""
        val result = SaxStreamingFeedParser(firstChunkSize = 1).parse(
            ByteArrayInputStream(rdf.toByteArray()), "https://example.com/feed.rdf",
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result

        assertEquals("One", result.entries.single().title)
    }

    @Test
    fun emptyFeedCompletesWithMetadataAndNoChunks() = runBlocking {
        val events = SaxStreamingFeedParser().parse(
            ByteArrayInputStream("<rss><channel><title>Empty</title></channel></rss>".toByteArray()),
            "https://example.com/feed.xml",
        ).toList()

        assertEquals(0, (events.filterIsInstance<FeedParseEvent.Completed>().single()).result.entries.size)
        assertTrue(events.any { it is FeedParseEvent.FeedMetadataAvailable })
        assertTrue(events.none { it is FeedParseEvent.EpisodeChunkAvailable })
    }

    @Test
    fun unknownExtensionsDoNotBreakParsingAndLimitsAreTerminal() = runBlocking {
        val xml = """<rss xmlns:x="urn:unknown"><channel><title>Example</title><x:future><x:value>ignored</x:value></x:future><item><guid>one</guid><title>One</title><enclosure url="https://example.com/one.mp3" type="audio/mpeg"/></item></channel></rss>"""
        val valid = SaxStreamingFeedParser(maxText = 8).parse(
            ByteArrayInputStream(xml.toByteArray()), "https://example.com/feed.xml",
        ).toList()
        assertEquals(1, valid.filterIsInstance<FeedParseEvent.Completed>().single().result.entries.size)

        val limited = SaxStreamingFeedParser(maxItems = 1).parse(
            ByteArrayInputStream(xml.replace("</item>", "</item><item><guid>two</guid></item>").toByteArray()),
            "https://example.com/feed.xml",
        ).toList()
        assertEquals(FailureCategory.LIMIT, limited.filterIsInstance<FeedParseEvent.Failed>().single().category)
        assertTrue(limited.none { it is FeedParseEvent.Completed })
    }

    @Test
    fun deeplyNestedUnknownExtensionsHitTheDepthLimit() = runBlocking {
        val xml = """
            <rss><channel><title>Deep</title><x1 xmlns="urn:one"><x2><x3><x4><x5>ignored</x5></x4></x3></x2></x1>
              <item><guid>one</guid><title>One</title><enclosure url="https://example.com/one.mp3" type="audio/mpeg"/></item>
            </channel></rss>
        """.trimIndent()

        val events = SaxStreamingFeedParser(maxDepth = 5).parse(
            ByteArrayInputStream(xml.toByteArray()), "https://example.com/feed.xml",
        ).toList()

        assertEquals(FailureCategory.LIMIT, (events.last() as FeedParseEvent.Failed).category)
        assertTrue(events.none { it is FeedParseEvent.Completed })
    }

    @Test
    fun parserResponseLimitIsTerminalAndDoesNotComplete() = runBlocking {
        val xml = "<rss><channel><title>Limited</title></channel></rss>"
        val events = SaxStreamingFeedParser(maxBytes = xml.toByteArray().size.toLong() - 1).parse(
            ByteArrayInputStream(xml.toByteArray()), "https://example.com/feed.xml",
        ).toList()

        assertEquals(FailureCategory.LIMIT, (events.last() as FeedParseEvent.Failed).category)
        assertTrue(events.none { it is FeedParseEvent.Completed })
    }

    @Test
    fun invalidFirstEnclosureDoesNotHideLaterPlayableEnclosure() = runBlocking {
        val xml = """
            <rss><channel><title>Enclosures</title><item>
              <guid>episode-1</guid><title>Playable episode</title>
              <enclosure url="file:///not-a-podcast-file.mp3" type="audio/mpeg"/>
              <enclosure url="https://example.com/episode.mp3" type="audio/mpeg" length="42"/>
            </item></channel></rss>
        """.trimIndent()

        val entry = SaxStreamingFeedParser().parse(
            ByteArrayInputStream(xml.toByteArray()), "https://example.com/feed.xml",
        ).filterIsInstance<FeedParseEvent.Completed>().toList().single().result.entries.single()

        assertEquals("https://example.com/episode.mp3", entry.audioUrl)
        assertEquals(42L, entry.audioSizeBytes)
    }

    @Test
    fun seededMalformedCorpusNeverEscapesOrProducesAmbiguousTerminalEvents() = runBlocking {
        val random = Random(0x50_4F_44_43_41_53_54)

        repeat(250) { caseNumber ->
            val xml = fuzzFeed(random)
            val events = withTimeout(500) {
                SaxStreamingFeedParser(
                    maxBytes = 4 * 1024,
                    maxItems = 16,
                    maxText = 1_024,
                    maxDepth = 16,
                ).parse(ByteArrayInputStream(xml.toByteArray()), "https://example.com/feed.xml").toList()
            }
            val terminalEvents = events.count {
                it is FeedParseEvent.Completed || it is FeedParseEvent.Failed
            }

            assertEquals("case $caseNumber", 1, terminalEvents)
            events.filterIsInstance<FeedParseEvent.Completed>().singleOrNull()?.result?.let { result ->
                result.entries.forEach { entry ->
                    assertTrue("case $caseNumber", entry.url.startsWith("http://") || entry.url.startsWith("https://"))
                    assertTrue(
                        "case $caseNumber",
                        entry.audioUrl == null || entry.audioUrl.startsWith("http://") || entry.audioUrl.startsWith("https://"),
                    )
                }
            }
        }
    }

    @Test
    fun cancellationStopsASlowInputStream() = runBlocking {
        val items = (1..50_000).joinToString("") { index ->
            "<item><guid>$index</guid><title>Episode $index</title><enclosure url=\"https://example.com/$index.mp3\" type=\"audio/mpeg\"/></item>"
        }
        val input = ByteArrayInputStream("<rss><channel><title>Slow</title>$items</channel></rss>".toByteArray())

        var timedOut = false
        try {
            withTimeout(1) {
                SaxStreamingFeedParser().parse(input, "https://example.com/feed.xml").collect { }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            timedOut = true
        }
        assertTrue(timedOut)
    }

    private fun fuzzFeed(random: Random): String {
        val items = (0 until random.nextInt(0, 8)).joinToString("") { index ->
            val enclosure = when (random.nextInt(4)) {
                0 -> "<enclosure url=\"file:///tmp/$index.mp3\" type=\"audio/mpeg\"/>"
                1 -> "<enclosure url=\"https://example.com/$index.mp3\" type=\"text/plain\"/>"
                2 -> "<enclosure url=\"https://example.com/$index.mp3\" type=\"audio/mpeg\" length=\"-1\"/>"
                else -> "<enclosure url=\"https://example.com/$index.mp3\" type=\"audio/mpeg\"/>"
            }
            "<item><guid>episode-$index</guid><title>Episode $index</title>" +
                "<description>Summary $index</description>$enclosure</item>"
        }
        val valid = "<rss xmlns:x=\"urn:fuzz\"><channel><title>Fuzz feed</title>$items" +
            "<x:extension><x:value>ignored</x:value></x:extension></channel></rss>"
        return when (random.nextInt(6)) {
            0 -> valid.take(random.nextInt(valid.length + 1))
            1 -> valid.replace("<title>Fuzz feed</title>", "<title>&broken;</title>")
            2 -> "<!DOCTYPE rss [<!ENTITY external SYSTEM \"file:///tmp/fuzz\">]>$valid"
            3 -> valid.replace(
                "<x:extension>",
                "<x:extension><x:a><x:b><x:c><x:d><x:e>",
            ).replace("</x:extension>", "</x:e></x:d></x:c></x:b></x:a></x:extension>")
            4 -> valid.replace("</channel>", "<item><guid>broken")
            else -> valid
        }
    }

    @Test
    fun zeroOrNegativeEnclosureSizesAreUnknown() {
        assertEquals(null, positiveSizeOrNull(0L))
        assertEquals(null, positiveSizeOrNull(-1L))
        assertEquals(1024L, positiveSizeOrNull(1024L))
    }

    @Test
    fun boundedResponseTextReaderAcceptsContentAtTheLimit() {
        val body = "podcast".toByteArray(StandardCharsets.UTF_8)

        assertEquals(
            "podcast",
            readBoundedText(ByteArrayInputStream(body), StandardCharsets.UTF_8, body.size.toLong()),
        )
    }

    @Test
    fun boundedResponseTextReaderRejectsContentAboveTheLimit() {
        assertThrows(FeedResponseTooLargeException::class.java) {
            readBoundedText(
                ByteArrayInputStream("podcast".toByteArray(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8,
                6L,
            )
        }
    }

    @Test
    fun boundedByteInputRejectsOversizedNotificationArtwork() {
        assertThrows(FeedResponseTooLargeException::class.java) {
            readBoundedBytes(
                ByteArrayInputStream(ByteArray(7)),
                6L,
            )
        }
    }

    @Test
    fun parsesFeedAndEpisodeExplicitFlagsWithEpisodeOverride() {
        val result = parseExplicitContent(
            """
                <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" version="2.0">
                  <channel>
                    <title>Example</title>
                    <link>https://example.com</link>
                    <itunes:explicit>true</itunes:explicit>
                    <item>
                      <guid>episode-1</guid>
                      <title>Clean episode</title>
                      <itunes:explicit>false</itunes:explicit>
                      <enclosure url="https://example.com/one.mp3" type="audio/mpeg" />
                    </item>
                    <item>
                      <guid>episode-2</guid>
                      <title>Explicit episode</title>
                      <enclosure url="https://example.com/two.mp3" type="audio/mpeg" />
                    </item>
                  </channel>
                </rss>
            """.trimIndent(),
        )

        assertEquals(true, result.feed)
        assertEquals(listOf(false, null), result.items)
    }

    @Test
    fun stableFeedIdsAreDeterministic() {
        assertEquals(
            stableFeedId("https://example.com/feed.xml"),
            stableFeedId("https://example.com/feed.xml"),
        )
        assertNotEquals(
            stableFeedId("https://example.com/feed.xml"),
            stableFeedId("https://example.com/other.xml"),
        )
    }

    @Test
    fun canonicalFeedUrlsNormalizeInputWithoutFragment() {
        assertEquals(
            "https://example.com/feed.xml",
            canonicalFeedUrl("example.com/feed.xml#latest"),
        )
        assertEquals(
            canonicalFeedUrl("https://example.com/feed.xml"),
            canonicalFeedUrl("https://EXAMPLE.com/feed.xml#latest"),
        )
    }

    @Test
    fun parsesRssPubDateWithTimezoneName() {
        val publishedAtMillis = "Tue, 02 Jan 2024 14:45:00 PST".parseFeedDateMillis()

        assertEquals(1_704_235_500_000L, publishedAtMillis)
    }

    @Test
    fun parsesBbcRfc1123GmtDate() {
        val publishedAtMillis = "Mon, 10 Aug 2026 05:00:07 GMT".parseFeedDateMillis()

        assertEquals(1_786_338_007_000L, publishedAtMillis)
    }

    @Test
    fun retainsUpstreamOrderAsAnIndependentField() {
        val entries = listOf(
            RssEntry(1L, 10L, "Example", "Editorial lead", 0, "https://example.com/lead", null, 1L, null, null, RssImageSource.NONE),
            RssEntry(2L, 10L, "Example", "Older item", 1, "https://example.com/older", null, 2L, null, null, RssImageSource.NONE),
        )

        assertEquals(listOf("Editorial lead", "Older item"), entries.sortedBy { it.feedOrder }.map { it.title })
    }

}
