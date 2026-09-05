package com.shapeshed.booth.data

data class PodcastCatalogIndex(
    val podcastsById: Map<Long, PodcastEntity>,
    val podcastTitlesById: Map<Long, String>,
    val availableTags: List<String>,
    val availableAppleCategories: List<String>,
    val subscribedFeedUrls: Set<String>,
)

fun buildPodcastCatalogIndex(podcasts: List<PodcastEntity>): PodcastCatalogIndex {
    val podcastsById = podcasts.associateBy(PodcastEntity::id)
    return PodcastCatalogIndex(
        podcastsById = podcastsById,
        podcastTitlesById = podcastsById.mapValues { (_, podcast) -> podcast.title },
        availableTags = podcastTags(podcasts),
        availableAppleCategories = podcastAppleCategories(podcasts),
        subscribedFeedUrls = podcasts.mapTo(mutableSetOf(), PodcastEntity::feedUrl),
    )
}
