package com.shapeshed.booth.ui

import com.shapeshed.booth.data.CategoryEntity
import com.shapeshed.booth.data.PodcastEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodcastHomeCategoryDiscoveryTest {

    @Test
    fun resolvesProviderCategoryIgnoringCaseAndWhitespace() {
        val podcast = podcastWithCategories(
            CategoryEntity(providerId = "apple", name = "News", externalId = "1309"),
        )

        val result = localTagDiscoveryCategory("  nEwS ", listOf(podcast))

        assertEquals("apple", result?.providerId)
        assertEquals("1309", result?.category?.id)
        assertEquals("nEwS", result?.category?.title)
    }

    @Test
    fun ignoresLocalCategoryAndCategoriesWithoutExternalIds() {
        val podcast = podcastWithCategories(
            CategoryEntity(providerId = "local", name = "News", externalId = "local-news"),
            CategoryEntity(providerId = "apple", name = "Sports"),
        )

        assertNull(localTagDiscoveryCategory("News", listOf(podcast)))
        assertNull(localTagDiscoveryCategory("", listOf(podcast)))
    }

    private fun podcastWithCategories(vararg categories: CategoryEntity): PodcastEntity =
        PodcastEntity(
            id = 1,
            title = "Test podcast",
            author = null,
            feedUrl = "https://example.com/feed.xml",
            siteUrl = null,
            descriptionHtml = null,
            artworkUrl = null,
            subscribedAtMillis = 0,
            lastRefreshMillis = null,
        ).also { it.categories = categories.toList() }
}
