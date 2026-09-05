package com.shapeshed.booth.ui

import com.shapeshed.booth.data.PodcastEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastLibrarySortingTest {
    @Test
    fun lastUpdatedSortUsesLatestEpisodeInsteadOfRefreshTime() {
        val olderEpisode = podcast(id = 1L, title = "Older episode")
        val newerEpisode = podcast(id = 2L, title = "Newer episode")

        val ordered = orderPodcasts(
            podcasts = listOf(olderEpisode, newerEpisode),
            sortOrder = PodcastSortOrder.LAST_UPDATED,
            latestEpisodePublishedAt = mapOf(
                olderEpisode.id to 100L,
                newerEpisode.id to 200L,
            ),
        )

        assertEquals(listOf(newerEpisode.id, olderEpisode.id), ordered.map { it.id })
    }

    @Test
    fun lastUpdatedSortPlacesMissingDatesLastAndBreaksTiesByTitle() {
        val zulu = podcast(id = 1L, title = "Zulu")
        val alpha = podcast(id = 2L, title = "Alpha")
        val noEpisodes = podcast(id = 3L, title = "No episodes")

        val ordered = orderPodcasts(
            podcasts = listOf(zulu, noEpisodes, alpha),
            sortOrder = PodcastSortOrder.LAST_UPDATED,
            latestEpisodePublishedAt = mapOf(
                zulu.id to 100L,
                alpha.id to 100L,
            ),
        )

        assertEquals(listOf(alpha.id, zulu.id, noEpisodes.id), ordered.map { it.id })
    }

    private fun podcast(id: Long, title: String) = PodcastEntity(
        id = id,
        title = title,
        author = null,
        feedUrl = "https://example.com/$id.xml",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
        subscribedAtMillis = 0L,
        lastRefreshMillis = 10_000L,
    )
}
