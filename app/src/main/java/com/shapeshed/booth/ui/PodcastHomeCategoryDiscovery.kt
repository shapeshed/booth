package com.shapeshed.booth.ui

import com.shapeshed.booth.data.PodcastDiscoveryCategory
import com.shapeshed.booth.data.PodcastEntity

internal data class ResolvedLocalDiscoveryCategory(
    val providerId: String,
    val category: PodcastDiscoveryCategory,
)

/** Resolves a local category label to the directory category used for discovery. */
internal fun localTagDiscoveryCategory(
    title: String,
    podcasts: List<PodcastEntity>,
): ResolvedLocalDiscoveryCategory? {
    val normalizedTitle = title.trim()
    if (normalizedTitle.isBlank()) return null

    val providerCategory = podcasts.asSequence()
        .flatMap { it.categories.asSequence() }
        .firstOrNull {
            it.providerId != "local" && it.name.equals(normalizedTitle, ignoreCase = true)
        }
        ?: return null
    val externalId = providerCategory.externalId ?: return null

    return ResolvedLocalDiscoveryCategory(
        providerId = providerCategory.providerId,
        category = PodcastDiscoveryCategory(
            id = externalId,
            title = normalizedTitle,
            appleGenreId = externalId,
        ),
    )
}
