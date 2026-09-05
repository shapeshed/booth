package com.shapeshed.booth.ui

import com.shapeshed.booth.data.PodcastEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastDetailSwipePagerTest {
    @Test
    fun selectedPodcastUsesItsListIndex() {
        val podcasts = listOf(
            PodcastEntity(
                id = 1L,
                title = "One",
                author = null,
                feedUrl = "https://one.example/feed",
                siteUrl = null,
                descriptionHtml = null,
                artworkUrl = null,
                subscribedAtMillis = 0L,
                lastRefreshMillis = null,
            ),
            PodcastEntity(
                id = 2L,
                title = "Two",
                author = null,
                feedUrl = "https://two.example/feed",
                siteUrl = null,
                descriptionHtml = null,
                artworkUrl = null,
                subscribedAtMillis = 0L,
                lastRefreshMillis = null,
            ),
        )

        assertEquals(1, podcastSwipeIndex(podcasts, 2L))
    }

    @Test
    fun missingPodcastFallsBackToFirstPage() {
        val podcasts = listOf(
            PodcastEntity(
                id = 1L,
                title = "One",
                author = null,
                feedUrl = "https://one.example/feed",
                siteUrl = null,
                descriptionHtml = null,
                artworkUrl = null,
                subscribedAtMillis = 0L,
                lastRefreshMillis = null,
            ),
        )

        assertEquals(0, podcastSwipeIndex(podcasts, 99L))
    }
}
