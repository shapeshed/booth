package com.shapeshed.booth.data

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
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
    fun providerExposesApplePagingContract() {
        assertEquals("apple", provider.id)
        assertEquals("Apple Podcasts", provider.displayName)
        assertEquals(true, provider.supportsCategoryPaging)
        assertEquals(true, provider.supportsPopularPodcasts)
    }

    private fun url(path: String): String = "http://127.0.0.1:${server.address.port}$path"

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
