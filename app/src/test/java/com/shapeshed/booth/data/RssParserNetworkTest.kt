package com.shapeshed.booth.data

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RssParserNetworkTest {
    private lateinit var server: HttpServer

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun transientHttpFailureIsSurfacedForWorkerRetryPolicy() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                exchange.sendResponseHeaders(503, 0)
                exchange.close()
            }

            val error = assertThrows(FeedHttpException::class.java) {
                runBlocking { Prof18FeedParser(OkHttpClient()).fetch(feedUrl()) }
            }

            assertEquals(503, error.statusCode)
        }
    }

    @Test
    fun htmlSuccessIsRejectedAsNotAFeed() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = "<html><body>not a feed</body></html>".toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            assertThrows(NotAFeedResponseException::class.java) {
                runBlocking { Prof18FeedParser(OkHttpClient()).fetch(feedUrl()) }
            }
        }
    }

    @Test
    fun notModifiedResponsePreservesValidators() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                exchange.responseHeaders.add("ETag", "new-etag")
                exchange.responseHeaders.add("Last-Modified", "yesterday")
                exchange.sendResponseHeaders(304, -1)
                exchange.close()
            }

            val result = Prof18FeedParser(OkHttpClient()).fetch(feedUrl(), "old-etag", "old-date")

            assertEquals(true, result.notModified)
            assertEquals("new-etag", result.etag)
            assertEquals("yesterday", result.lastModified)
        }
    }

    @Test
    fun streamingCompleteProviderIsPrimaryAndPreservesResponseValidators() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = """
                    <rss version="2.0">
                      <channel>
                        <title>Streaming show</title>
                        <link>https://example.com/show</link>
                        <item>
                          <guid>episode-1</guid>
                          <title>Episode one</title>
                          <enclosure url="/audio/one.mp3" type="audio/mpeg" length="123" />
                        </item>
                      </channel>
                    </rss>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.responseHeaders.add("ETag", "streaming-etag")
                exchange.responseHeaders.add("Last-Modified", "today")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            var fallbackCalls = 0
            val fallback = testFallbackProvider {
                fallbackCalls++
            }
            val result = StreamingCompletePodcastFeedProvider(
                client = OkHttpClient(),
                parser = SaxStreamingFeedParser(),
                fallback = fallback,
            ).fetch(feedUrl())

            assertEquals("Streaming show", result.podcast.title)
            assertEquals("Episode one", result.episodes.single().title)
            assertEquals("streaming-etag", result.etag)
            assertEquals("today", result.lastModified)
            assertEquals(0, fallbackCalls)
        }
    }

    @Test
    fun streamingSuccessRecordsSafeParserMetrics() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = "<rss><channel><title>Metrics</title></channel></rss>".toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            val metrics = mutableListOf<FeedParserMetric>()
            StreamingCompletePodcastFeedProvider(
                client = OkHttpClient(),
                parser = SaxStreamingFeedParser(),
                fallback = testFallbackProvider {},
                metrics = FeedParserMetrics { metrics += it },
            ).fetch(feedUrl())

            assertEquals(1, metrics.size)
            assertEquals(FeedParserMode.STREAMING, metrics.single().mode)
            assertEquals(0, metrics.single().itemCount)
            assertEquals(null, metrics.single().failureCategory)
            assertTrue(metrics.single().durationMs >= 0L)
        }
    }

    @Test
    fun streamingCompleteProviderFallsBackWithoutReturningPartialResults() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = "<rss><channel><title>Broken".toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            var fallbackCalls = 0
            val fallback = testFallbackProvider {
                fallbackCalls++
            }
            val result = StreamingCompletePodcastFeedProvider(
                client = OkHttpClient(),
                parser = SaxStreamingFeedParser(),
                fallback = fallback,
            ).fetch(feedUrl())

            assertEquals("Fallback show", result.podcast.title)
            assertTrue(result.episodes.isEmpty())
            assertEquals(1, fallbackCalls)
        }
    }

    @Test
    fun parserFailurePassesResponseValidatorsToFallback() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = "<rss><channel><title>Broken".toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.responseHeaders.add("ETag", "response-etag")
                exchange.responseHeaders.add("Last-Modified", "response-date")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            var receivedUrl: String? = null
            var receivedEtag: String? = null
            var receivedLastModified: String? = null
            val metrics = mutableListOf<FeedParserMetric>()
            val result = StreamingCompletePodcastFeedProvider(
                client = OkHttpClient(),
                parser = SaxStreamingFeedParser(),
                fallback = testFallbackProvider(
                    onFetch = {},
                    onArguments = { url, etag, lastModified ->
                        receivedUrl = url
                        receivedEtag = etag
                        receivedLastModified = lastModified
                    },
                ),
                metrics = FeedParserMetrics { metrics += it },
            ).fetch(feedUrl(), etag = "old-etag", lastModified = "old-date")

            assertEquals("Fallback show", result.podcast.title)
            assertEquals(feedUrl(), receivedUrl)
            assertEquals("response-etag", receivedEtag)
            assertEquals("response-date", receivedLastModified)
            assertEquals(FeedParserMode.FALLBACK, metrics.single().mode)
            assertEquals(FailureCategory.STRUCTURAL, metrics.single().failureCategory)
        }
    }

    @Test
    fun htmlResponseUsesFallbackInsteadOfStreamingParser() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = "<html><body>not a podcast feed</body></html>".toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            var fallbackCalls = 0
            val result = StreamingCompletePodcastFeedProvider(
                client = OkHttpClient(),
                parser = SaxStreamingFeedParser(),
                fallback = testFallbackProvider { fallbackCalls++ },
            ).fetch(feedUrl())

            assertEquals("Fallback show", result.podcast.title)
            assertEquals(1, fallbackCalls)
        }
    }

    @Test
    fun redirectUsesFinalResponseUrlForTheParsedFeed() {
        runBlocking {
            val finalUrl = "http://127.0.0.1:${server.address.port}/canonical"
            server.createContext("/feed") { exchange ->
                exchange.responseHeaders.add("Location", finalUrl)
                exchange.sendResponseHeaders(302, -1)
                exchange.close()
            }
            server.createContext("/canonical") { exchange ->
                val body = """
                    <rss><channel><title>Redirected show</title>
                      <item><guid>redirected</guid><title>Episode</title>
                        <enclosure url="/episode.mp3" type="audio/mpeg"/>
                      </item>
                    </channel></rss>
                """.trimIndent().toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            val result = StreamingCompletePodcastFeedProvider(
                client = OkHttpClient(),
                parser = SaxStreamingFeedParser(),
                fallback = testFallbackProvider {},
            ).fetch(feedUrl())

            assertEquals(finalUrl, result.podcast.feedUrl)
            assertEquals("http://127.0.0.1:${server.address.port}/episode.mp3", result.episodes.single().audioUrl)
        }
    }

    @Test
    fun streamingCompleteProviderHidesPartialParseBeforeFallback() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = """
                    <rss><channel><title>Partial show</title>
                      <item><guid>partial</guid><title>Partial episode</title>
                        <enclosure url="https://example.com/partial.mp3" type="audio/mpeg"/>
                      </item>
                """.trimIndent().toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            var fallbackCalls = 0
            val result = StreamingCompletePodcastFeedProvider(
                client = OkHttpClient(),
                parser = SaxStreamingFeedParser(firstChunkSize = 1),
                fallback = testFallbackProvider { fallbackCalls++ },
            ).fetch(feedUrl())

            assertEquals("Fallback show", result.podcast.title)
            assertTrue(result.episodes.isEmpty())
            assertEquals(1, fallbackCalls)
        }
    }

    @Test
    fun streamingDiscoveryReplacesPartialUpdatesAfterFallback() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = """
                    <rss><channel><title>Partial show</title>
                      <item><guid>partial</guid><title>Partial episode</title>
                        <enclosure url="https://example.com/partial.mp3" type="audio/mpeg"/>
                      </item>
                """.trimIndent().toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            var fallbackCalls = 0
            val updates = mutableListOf<String>()
            val result = StreamingPodcastFeedProvider(
                client = OkHttpClient(),
                parser = SaxStreamingFeedParser(firstChunkSize = 1),
                fallback = testFallbackProvider { fallbackCalls++ },
            ).fetch(feedUrl()) { update -> updates += update.podcast.title }

            assertEquals("Fallback show", result.podcast.title)
            assertEquals(listOf("Partial show", "Partial show", "Fallback show"), updates)
            assertEquals(1, fallbackCalls)
        }
    }

    @Test
    fun streamingDiscoveryRecordsFallbackAndPreservesResponseValidators() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = "<rss><channel><title>Broken".toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.responseHeaders.add("ETag", "discovery-etag")
                exchange.responseHeaders.add("Last-Modified", "discovery-date")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            var receivedEtag: String? = null
            var receivedLastModified: String? = null
            val metrics = mutableListOf<FeedParserMetric>()
            StreamingPodcastFeedProvider(
                client = OkHttpClient(),
                parser = SaxStreamingFeedParser(),
                fallback = testFallbackProvider(
                    onFetch = {},
                    onArguments = { _, etag, lastModified ->
                        receivedEtag = etag
                        receivedLastModified = lastModified
                    },
                ),
                metrics = FeedParserMetrics { metrics += it },
            ).fetch(feedUrl()) {}

            assertEquals("discovery-etag", receivedEtag)
            assertEquals("discovery-date", receivedLastModified)
            assertEquals(FeedParserMode.FALLBACK, metrics.single().mode)
            assertEquals(FailureCategory.STRUCTURAL, metrics.single().failureCategory)
        }
    }

    @Test
    fun cancellationDuringFallbackCancelsFallbackAndDoesNotReturnAResult() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = "<rss><channel><title>Broken".toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            val fallbackStarted = CompletableDeferred<Unit>()
            val fallback = object : PodcastFeedProvider {
                override suspend fun fetch(
                    feedUrl: String,
                    etag: String?,
                    lastModified: String?,
                    onEpisodeProgress: (processed: Int, total: Int) -> Unit,
                ): PodcastFeed {
                    fallbackStarted.complete(Unit)
                    awaitCancellation()
                }
            }
            val job = launch {
                StreamingCompletePodcastFeedProvider(
                    client = OkHttpClient(),
                    parser = SaxStreamingFeedParser(),
                    fallback = fallback,
                ).fetch(feedUrl())
            }

            fallbackStarted.await()
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
        }
    }

    @Test
    fun cancellationDuringDiscoveryParsingStopsWithoutFallbackOrStaleUpdate() {
        runBlocking {
            server.createContext("/feed") { exchange ->
                val body = "<rss><channel><title>Slow</title></channel></rss>"
                    .toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/rss+xml")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }

            val parserStarted = CompletableDeferred<Unit>()
            var fallbackCalls = 0
            var updateCalls = 0
            val parser = object : StreamingFeedParser {
                override fun parse(input: java.io.InputStream, feedUrl: String) = flow<FeedParseEvent> {
                    parserStarted.complete(Unit)
                    awaitCancellation()
                }
            }
            val job = launch {
                StreamingPodcastFeedProvider(
                    client = OkHttpClient(),
                    parser = parser,
                    fallback = testFallbackProvider { fallbackCalls++ },
                ).fetch(feedUrl()) { updateCalls++ }
            }

            parserStarted.await()
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertEquals(0, fallbackCalls)
            assertEquals(0, updateCalls)
        }
    }

    private fun testFallbackProvider(
        onArguments: (String, String?, String?) -> Unit = { _, _, _ -> },
        onFetch: () -> Unit,
    ): PodcastFeedProvider =
        object : PodcastFeedProvider {
            override suspend fun fetch(
                feedUrl: String,
                etag: String?,
                lastModified: String?,
                onEpisodeProgress: (processed: Int, total: Int) -> Unit,
            ): PodcastFeed {
                onFetch()
                onArguments(feedUrl, etag, lastModified)
                return PodcastFeed(
                    podcast = Podcast(
                        id = podcastId(feedUrl),
                        title = "Fallback show",
                        author = null,
                        feedUrl = feedUrl,
                        siteUrl = null,
                        descriptionHtml = null,
                        artworkUrl = null,
                    ),
                    episodes = emptyList(),
                )
            }
        }

    private fun feedUrl(): String = "http://127.0.0.1:${server.address.port}/feed"
}
