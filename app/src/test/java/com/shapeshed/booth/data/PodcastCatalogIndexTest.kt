package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastCatalogIndexTest {
    @Test
    fun indexBuildsStableLookupMapsTagsAndFeeds() {
        val podcasts = listOf(
            podcast(1L, "Zulu", "news, tech"),
            podcast(2L, "Alpha", "tech, history"),
        )

        val index = buildPodcastCatalogIndex(podcasts)

        assertEquals("Zulu", index.podcastsById[1L]?.title)
        assertEquals(mapOf(1L to "Zulu", 2L to "Alpha"), index.podcastTitlesById)
        assertEquals(listOf("history", "news", "tech"), index.availableTags)
        assertEquals(setOf("https://example.com/1", "https://example.com/2"), index.subscribedFeedUrls)
    }

    private fun podcast(id: Long, title: String, tags: String) = PodcastEntity(
        id = id,
        title = title,
        author = null,
        feedUrl = "https://example.com/$id",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
        subscribedAtMillis = 0L,
        lastRefreshMillis = null,
    ).also {
        it.categories = tags.split(',').map(String::trim).map { name ->
            CategoryEntity(providerId = LOCAL_DIRECTORY_PROVIDER_ID, name = name)
        }
    }
}
