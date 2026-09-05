package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastTagsTest {
    @Test
    fun podcastTagsTrimsDeduplicatesAndSortsValues() {
        val podcasts = listOf(
            podcast(1L, " news, comedy "),
            podcast(2L, "Comedy, technology,, "),
        )

        assertEquals(listOf("Comedy", "comedy", "news", "technology"), podcastTags(podcasts))
    }

    @Test
    fun displayCategoriesPrefersLocalTagsAndFallsBackToAppleCategories() {
        val tagged = podcast(1L, "local", encodeAppleCategories(listOf("News")))
        val untagged = podcast(2L, "", encodeAppleCategories(listOf("Technology", "Podcasts")))

        assertEquals(listOf("local"), tagged.displayCategories())
        assertEquals(listOf("Technology", "Podcasts"), untagged.displayCategories())
        assertEquals(listOf("local", "News"), tagged.searchableCategories())
    }

    @Test
    fun normalizedCategoriesUseLocalForDisplayAndAllProvidersForSearch() {
        val podcast = podcast(3L, "").also {
            it.categories = listOf(
                CategoryEntity(1L, LOCAL_DIRECTORY_PROVIDER_ID, "Documentaries"),
                CategoryEntity(2L, APPLE_DIRECTORY_PROVIDER_ID, "Society & Culture", "1324"),
            )
        }

        assertEquals(listOf("Documentaries"), podcast.displayCategories())
        assertEquals(listOf("Documentaries", "Society & Culture"), podcast.searchableCategories())
        assertEquals("1324", podcast.categoryExternalId(APPLE_DIRECTORY_PROVIDER_ID, "Society & Culture"))
    }

    @Test
    fun appleCategoriesRoundTripAsJson() {
        val categories = listOf("News", "Government", "News")

        assertEquals(listOf("News", "Government"), decodeAppleCategories(encodeAppleCategories(categories)))
    }

    @Test
    fun appleCategoryIdsRoundTrip() {
        val categoryIds = mapOf("News" to "1309", "Technology" to "1318")

        assertEquals(categoryIds, decodeAppleCategoryIds(encodeAppleCategoryIds(categoryIds)))
    }

    private fun podcast(id: Long, tags: String, appleCategories: String = "") = PodcastEntity(
        id = id,
        title = "Podcast $id",
        author = null,
        feedUrl = "https://example.com/$id.xml",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
        tags = tags,
        appleCategories = appleCategories,
        subscribedAtMillis = 0L,
        lastRefreshMillis = null,
    ).also {
        it.categories = tags.split(',').map(String::trim).filter(String::isNotBlank)
            .map { name -> CategoryEntity(providerId = LOCAL_DIRECTORY_PROVIDER_ID, name = name) } +
            decodeAppleCategories(appleCategories).map { name ->
                CategoryEntity(providerId = APPLE_DIRECTORY_PROVIDER_ID, name = name)
            }
    }
}
