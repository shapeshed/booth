package com.shapeshed.booth.ui

import com.shapeshed.booth.data.CategoryEntity
import com.shapeshed.booth.data.PodcastEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodcastHomeEpisodeFilteringTest {
    @Test
    fun emptyTagsDoNotRestrictEpisodeCatalogue() {
        assertNull(podcastIdsForEpisodeTags(listOf(podcast(1)), emptySet()))
    }

    @Test
    fun returnsPodcastsMatchingAnySelectedCategory() {
        val podcasts = listOf(
            podcast(1, "News"),
            podcast(2, "Sports"),
            podcast(3, "Arts"),
        )

        assertEquals(listOf(1L, 2L), podcastIdsForEpisodeTags(podcasts, setOf("News", "Sports")))
    }

    private fun podcast(id: Long, tags: String = "") = PodcastEntity(
        id = id,
        title = "Podcast $id",
        author = null,
        feedUrl = "https://example.com/$id.xml",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
        tags = tags,
        subscribedAtMillis = 0,
        lastRefreshMillis = null,
    ).also { podcast ->
        if (tags.isNotBlank()) {
            podcast.categories = listOf(CategoryEntity(providerId = "local", name = tags))
        }
    }
}
