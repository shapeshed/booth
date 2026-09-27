package com.shapeshed.booth.data

/**
 * Pure conversions between the Room entities and the domain model used by the UI and by the
 * feed providers.
 *
 * These were file-private functions at the bottom of PodcastRepository, which is a 700-line class
 * whose constructor needs a DAO, an HTTP client and a feed provider. Nothing about a field rename
 * or a category mapping is worth standing that up for, and leaving them there is why they had no
 * tests: `podcastFtsQuery` is the only thing standing between raw user input and an FTS4 MATCH
 * string, and it was untested.
 *
 * No Android, no IO and no clock, so all of it is plain JVM-testable.
 */
internal fun toSearchEntity(episode: EpisodeEntity): EpisodeSearchFtsEntity = EpisodeSearchFtsEntity(
    rowId = episode.id,
    title = episode.title,
    descriptionHtml = episode.descriptionHtml.orEmpty(),
    podcastId = episode.podcastId.toString(),
)

internal fun PodcastEntity.toPreviewPodcast() = Podcast(
    id = id,
    title = title,
    author = author,
    feedUrl = feedUrl,
    siteUrl = siteUrl,
    descriptionHtml = descriptionHtml,
    artworkUrl = artworkUrl,
    explicit = explicit,
    categories = displayCategories(),
    categoryIds = categories.mapNotNull { category ->
        category.externalId?.let { category.name to it }
    }.toMap(),
)

internal fun EpisodeEntity.toPreviewEpisode() = Episode(
    id = id,
    podcastId = podcastId,
    guid = guid,
    title = title,
    descriptionHtml = descriptionHtml,
    audioUrl = audioUrl,
    mimeType = mimeType,
    artworkUrl = artworkUrl,
    publishedAtMillis = publishedAtMillis,
    durationMs = durationMs,
    linkUrl = linkUrl,
    videoUrl = videoUrl,
    videoMimeType = videoMimeType,
    audioSizeBytes = audioSizeBytes,
    videoSizeBytes = videoSizeBytes,
    explicit = explicit,
)

/**
 * Turns raw user input into an FTS4 MATCH expression.
 *
 * Every token is reduced to letters and digits, so FTS operators, quotes and the `-`/`*` syntax a
 * user might type cannot reach the query language. Tokens are joined with AND and given a prefix
 * wildcard so search feels incremental. Returns null when nothing searchable remains, which the
 * caller treats as "no results" rather than as a query matching everything.
 */
internal fun podcastFtsQuery(query: String): String? = query.trim()
    .split(Regex("\\s+"))
    .map { it.filter(Char::isLetterOrDigit) }
    .filter(String::isNotBlank)
    .takeIf(List<String>::isNotEmpty)
    ?.joinToString(" AND ") { "$it*" }
