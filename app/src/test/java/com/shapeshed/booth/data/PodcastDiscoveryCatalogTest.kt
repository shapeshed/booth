package com.shapeshed.booth.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastDiscoveryCatalogTest {
    @Test
    fun loadCategoryPassesOffsetAndKeepsCategoryTitle() = runBlocking {
        val provider = FakeProvider()
        val category = PodcastDiscoveryCategory("news", "News", "1309")

        val result = DefaultPodcastDiscoveryCatalog(provider).load(category, offset = 25)

        assertEquals(25, provider.lastOffset)
        assertEquals("News", result.title)
        assertEquals(listOf("page-25"), result.results.map { it.podcast.title })
    }

    private class FakeProvider : PodcastDiscoveryProvider {
        var lastOffset = -1
        override val id: String = "fake"

        override suspend fun browse(shelf: PodcastDiscoveryShelf): List<PodcastSearchResult> = emptyList()

        override suspend fun browse(category: PodcastDiscoveryCategory): List<PodcastSearchResult> =
            browse(category, 0)

        override suspend fun browse(
            category: PodcastDiscoveryCategory,
            offset: Int,
        ): List<PodcastSearchResult> {
            lastOffset = offset
            return listOf(
                PodcastSearchResult(
                    providerId = id,
                    podcast = Podcast(
                        id = offset.toLong(),
                        title = "page-$offset",
                        author = null,
                        feedUrl = "https://example.com/$offset.xml",
                        siteUrl = null,
                        descriptionHtml = null,
                        artworkUrl = null,
                    ),
                ),
            )
        }
    }
}
