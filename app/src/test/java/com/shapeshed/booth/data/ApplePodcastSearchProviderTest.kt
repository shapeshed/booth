package com.shapeshed.booth.data

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ApplePodcastSearchProviderTest {
    private lateinit var server: HttpServer
    private lateinit var provider: ApplePodcastSearchProvider

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
        provider = ApplePodcastSearchProvider(
            client = OkHttpClient(),
            localeProvider = { Locale.UK },
            apiBaseUrl = url(""),
            rssBaseUrl = url(""),
        )
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun blankSearchDoesNotMakeARequest() = runBlocking {
        assertEquals(emptyList<PodcastSearchResult>(), provider.search("  "))
    }

    @Test
    fun unsuccessfulSearchResponseIsSurfaced() {
        server.createContext("/search") { exchange -> respond(exchange, 503, "{}") }

        assertThrows(RuntimeException::class.java) {
            runBlocking { provider.search("news") }
        }
    }

    @Test
    fun searchReturnsParsedResultsFromTheResponse() = runBlocking {
        server.createContext("/search") { exchange ->
            respond(
                exchange,
                200,
                """
                {"results":[{
                  "feedUrl":"https://example.com/feed.xml",
                  "collectionName":"Example Show",
                  "artistName":"Example Author"
                }]}
                """.trimIndent(),
            )
        }

        val results = provider.search("news")

        assertEquals(1, results.size)
        assertEquals("Example Show", results.single().podcast.title)
    }

    /**
     * Ids are matched by parsing them out of the collectionViewUrl, not by substring.
     *
     * "id123456" is a prefix of "id1234567", so the old `contains("id$id")` matched the longer id
     * first and paired the wrong show with the requested one. A Top-Shows lookup sends a batch of
     * ids at once, which is exactly where that bites.
     */
    @Test
    fun lookupPairsEachIdWithItsOwnEntryWhenIdsShareAPrefix() = runBlocking {
        server.createContext("/lookup") { exchange ->
            respond(
                exchange,
                200,
                """
                {"results":[
                  {"feedUrl":"https://example.com/long.xml","collectionName":"Long","collectionViewUrl":"https://podcasts.apple.com/us/podcast/long/id1234567"},
                  {"feedUrl":"https://example.com/short.xml","collectionName":"Short","collectionViewUrl":"https://podcasts.apple.com/us/podcast/short/id123456"}
                ]}
                """.trimIndent(),
            )
        }

        val results = provider.lookup(listOf("123456", "1234567"))

        assertEquals(listOf("Short", "Long"), results.map { it.podcast.title })
    }

    @Test
    fun lookupIgnoresTheEpisodeIdQueryParameter() = runBlocking {
        server.createContext("/lookup") { exchange ->
            respond(
                exchange,
                200,
                """
                {"results":[
                  {"feedUrl":"https://example.com/one.xml","collectionName":"One","collectionViewUrl":"https://podcasts.apple.com/us/podcast/one/id42?i=999"}
                ]}
                """.trimIndent(),
            )
        }

        val results = provider.lookup(listOf("42"))

        assertEquals(listOf("One"), results.map { it.podcast.title })
    }

    @Test
    fun lookupOmitsIdsWithNoMatchingEntry() = runBlocking {
        server.createContext("/lookup") { exchange ->
            respond(
                exchange,
                200,
                """
                {"results":[
                  {"feedUrl":"https://example.com/one.xml","collectionName":"One","collectionViewUrl":"https://podcasts.apple.com/us/podcast/one/id1"}
                ]}
                """.trimIndent(),
            )
        }

        // "2" has no entry, so it is dropped rather than paired with "1". The result is shorter
        // than the request, which is the pre-existing contract: lookup is best effort and callers
        // treat a short list as "some of these are not on the directory".
        assertEquals(listOf("One"), provider.lookup(listOf("1", "2")).map { it.podcast.title })
    }

    @Test
    fun providerExposesApplePagingContract() {
        assertEquals("apple", provider.id)
        assertEquals("Apple Podcasts", provider.displayName)
        assertEquals(true, provider.supportsCategoryPaging)
        assertEquals(true, provider.supportsPopularPodcasts)
    }

    // These three need a real org.json on the unit-test classpath. android.jar's org.json is a
    // stub, so before testImplementation 'org.json:json' every one of them passed vacuously
    // against a parser that returned empty for any input.

    @Test
    fun parseLookupResultsReadsFeedTitleAuthorAndArtwork() {
        val results = provider.parseLookupResults(
            JSONObject(
                """
                {"results":[{
                  "feedUrl":"https://example.com/feed.xml",
                  "collectionName":"Example Show",
                  "artistName":"Example Author",
                  "collectionViewUrl":"https://podcasts.apple.com/us/podcast/x/id42",
                  "artworkUrl600":"https://example.com/600.png",
                  "artworkUrl100":"https://example.com/100.png",
                  "primaryGenreName":"Technology",
                  "primaryGenreId":"1311",
                  "genres":["Technology","News"],
                  "genreIds":["1311","1316"]
                }]}
                """.trimIndent(),
            ),
        )

        assertEquals(1, results.size)
        val podcast = results.single().podcast
        assertEquals("Example Show", podcast.title)
        assertEquals("Example Author", podcast.author)
        assertEquals("https://example.com/600.png", podcast.artworkUrl)
        assertEquals("https://podcasts.apple.com/us/podcast/x/id42", podcast.siteUrl)
        assertEquals(listOf("Technology", "News"), podcast.categories)
        assertEquals(
            mapOf("Technology" to "1311", "News" to "1316"),
            podcast.categoryIds,
        )
    }

    @Test
    fun parseLookupResultsSkipsEntriesWithoutAFeedUrl() {
        val results = provider.parseLookupResults(
            JSONObject(
                """
                {"results":[
                  {"collectionName":"No Feed"},
                  {"feedUrl":"   ","collectionName":"Blank Feed"},
                  {"feedUrl":"https://example.com/keep.xml","collectionName":"Kept"}
                ]}
                """.trimIndent(),
            ),
        )

        assertEquals(listOf("Kept"), results.map { it.podcast.title })
    }

    @Test
    fun parseLookupResultsFallsBackToArtworkUrl100() {
        val results = provider.parseLookupResults(
            JSONObject(
                """
                {"results":[{
                  "feedUrl":"https://example.com/feed.xml",
                  "collectionName":"Example Show",
                  "artworkUrl600":"",
                  "artworkUrl100":"https://example.com/100.png"
                }]}
                """.trimIndent(),
            ),
        )

        assertEquals("https://example.com/100.png", results.single().podcast.artworkUrl)
    }

    private fun url(path: String): String = "http://127.0.0.1:${server.address.port}$path"

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
