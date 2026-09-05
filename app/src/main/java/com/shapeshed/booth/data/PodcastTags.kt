package com.shapeshed.booth.data

const val LOCAL_DIRECTORY_PROVIDER_ID = "local"
const val APPLE_DIRECTORY_PROVIDER_ID = "apple"
const val PODCAST_INDEX_DIRECTORY_PROVIDER_ID = "podcast-index"

data class PodcastCategory(
    val providerId: String,
    val name: String,
    val externalId: String? = null,
    val parentId: Long? = null,
)

internal fun String?.normalizedPodcastTags(): String? = this
    ?.split(',', '#')
    ?.map(String::trim)
    ?.filter(String::isNotBlank)
    ?.distinctBy { it.lowercase() }
    ?.joinToString(", ")
    ?.takeIf(String::isNotBlank)

internal fun encodeAppleCategories(categories: List<String>): String = categories
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinct()
    .joinToString(AppleCategorySeparator)

internal fun decodeAppleCategories(value: String): List<String> = runCatching {
    value.split(AppleCategorySeparator).map(String::trim).filter(String::isNotBlank).distinct()
}.getOrDefault(emptyList())

internal fun encodeAppleCategoryIds(categoryIds: Map<String, String>): String = categoryIds
    .mapKeys { it.key.trim() }
    .filter { (name, id) -> name.isNotBlank() && id.isNotBlank() }
    .entries
    .joinToString(AppleCategorySeparator) { "${it.key}$AppleCategoryIdSeparator${it.value}" }

internal fun decodeAppleCategoryIds(value: String): Map<String, String> = value
    .split(AppleCategorySeparator)
    .mapNotNull { entry ->
        val parts = entry.split(AppleCategoryIdSeparator, limit = 2)
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0] to parts[1] else null
    }
    .toMap()

private const val AppleCategorySeparator = "\u001F"
private const val AppleCategoryIdSeparator = "\u001E"

internal fun PodcastEntity.displayCategories(): List<String> =
    categories.filter { it.providerId == LOCAL_DIRECTORY_PROVIDER_ID }.map(CategoryEntity::name)
        .ifEmpty { categories.filter { it.providerId != LOCAL_DIRECTORY_PROVIDER_ID }.map(CategoryEntity::name) }

internal fun PodcastEntity.searchableCategories(): List<String> =
    categories.map(CategoryEntity::name)
        .filter(String::isNotBlank)
        .distinct()

fun podcastTags(podcasts: List<PodcastEntity>): List<String> = podcasts
    .flatMap { podcast ->
        podcast.categories.filter { it.providerId == LOCAL_DIRECTORY_PROVIDER_ID }.map(CategoryEntity::name)
    }
    .distinct()
    .sorted()

fun podcastAppleCategories(podcasts: List<PodcastEntity>): List<String> = podcasts
    .flatMap { podcast ->
        podcast.categories.filter { it.providerId == APPLE_DIRECTORY_PROVIDER_ID }.map(CategoryEntity::name)
    }
    .distinct()
    .sorted()

fun podcastCategories(podcasts: List<PodcastEntity>): List<String> = podcasts
    .flatMap(PodcastEntity::searchableCategories)
    .distinct()
    .sorted()

internal fun PodcastEntity.hasLocalCategories(): Boolean =
    categories.any { it.providerId == LOCAL_DIRECTORY_PROVIDER_ID }

internal fun PodcastEntity.categoryExternalId(providerId: String, name: String): String? =
    categories.firstOrNull { it.providerId == providerId && it.name == name }?.externalId
