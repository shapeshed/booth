package com.shapeshed.booth.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastNavigationKeyTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun episodeDestination_roundTripsThroughSavedStateSerialization() {
        val destination = PodcastNavigationKey.EpisodeDetail(
            episodeId = 42L,
            origin = EpisodeNavigationOrigin.Search,
        )

        val encoded = json.encodeToString(PodcastNavigationKey.serializer(), destination)
        val restored = json.decodeFromString(PodcastNavigationKey.serializer(), encoded)

        assertEquals(destination, restored)
    }

    @Test
    fun categoryDestination_preservesProviderIdentity() {
        val destination = PodcastNavigationKey.Category(
            providerId = "podcast-index",
            categoryId = "politics",
            title = "Politik",
        )

        val encoded = json.encodeToString(PodcastNavigationKey.serializer(), destination)
        val restored = json.decodeFromString(PodcastNavigationKey.serializer(), encoded)

        assertEquals(destination, restored)
    }

    @Test
    fun topLevelDestinations_roundTripToPodcastTabs() {
        assertEquals(PodcastTab.HOME, PodcastTab.HOME.toNavigationKey().toPodcastTab())
        assertEquals(PodcastTab.UP_NEXT, PodcastTab.UP_NEXT.toNavigationKey().toPodcastTab())
        assertEquals(
            PodcastTab.SUBSCRIPTIONS,
            PodcastTab.SUBSCRIPTIONS.toNavigationKey().toPodcastTab(),
        )
    }

    @Test
    fun secondaryDestinations_roundTripWithoutLosingIdentity() {
        val destinations = listOf(
            PodcastNavigationKey.Settings,
            PodcastNavigationKey.GlobalSearch,
            PodcastNavigationKey.DiscoveryPodcast("https://example.com/feed.xml"),
            PodcastNavigationKey.DiscoveryCategory("podcast-index", "politics", "Politik"),
            PodcastNavigationKey.DiscoveryEpisode(99L),
        )

        destinations.forEach { destination ->
            val encoded = json.encodeToString(PodcastNavigationKey.serializer(), destination)
            val restored = json.decodeFromString(PodcastNavigationKey.serializer(), encoded)
            assertEquals(destination, restored)
        }
    }

    @Test
    fun searchAndDiscoveryDestinations_preserveTheirOrigin() {
        val stack = mutableListOf<PodcastNavigationKey>(
            PodcastNavigationKey.Subscriptions,
            PodcastNavigationKey.GlobalSearch,
            PodcastNavigationKey.Discovery,
            PodcastNavigationKey.DiscoveryPodcast("https://example.com/feed.xml"),
            PodcastNavigationKey.DiscoveryCategory("apple", "technology", "Technology"),
        )

        assertEquals(
            PodcastNavigationKey.DiscoveryCategory("apple", "technology", "Technology"),
            stack.removeAt(stack.lastIndex),
        )
        assertEquals(
            PodcastNavigationKey.DiscoveryPodcast("https://example.com/feed.xml"),
            stack.removeAt(stack.lastIndex),
        )
        assertEquals(PodcastNavigationKey.Discovery, stack.removeAt(stack.lastIndex))
        assertEquals(PodcastNavigationKey.GlobalSearch, stack.removeAt(stack.lastIndex))
        assertEquals(listOf(PodcastNavigationKey.Subscriptions), stack)
    }

    @Test
    fun nestedDetailDestinations_popInTheOrderTheyWereOpened() {
        val stack = mutableListOf<PodcastNavigationKey>(
            PodcastNavigationKey.Inbox,
            PodcastNavigationKey.EpisodeDetail(1L, EpisodeNavigationOrigin.Inbox),
            PodcastNavigationKey.PodcastDetail(10L),
            PodcastNavigationKey.EpisodeDetail(2L, EpisodeNavigationOrigin.Podcast),
            PodcastNavigationKey.PodcastDetail(10L),
        )

        assertEquals(PodcastNavigationKey.PodcastDetail(10L), stack.removeAt(stack.lastIndex))
        assertEquals(
            PodcastNavigationKey.EpisodeDetail(2L, EpisodeNavigationOrigin.Podcast),
            stack.removeAt(stack.lastIndex),
        )
        assertEquals(PodcastNavigationKey.PodcastDetail(10L), stack.removeAt(stack.lastIndex))
        assertEquals(
            PodcastNavigationKey.EpisodeDetail(1L, EpisodeNavigationOrigin.Inbox),
            stack.removeAt(stack.lastIndex),
        )
        assertEquals(listOf(PodcastNavigationKey.Inbox), stack)
    }
}
