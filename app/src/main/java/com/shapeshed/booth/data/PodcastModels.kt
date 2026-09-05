package com.shapeshed.booth.data


data class Podcast(
    val id: Long,
    val title: String,
    val author: String?,
    val feedUrl: String,
    val siteUrl: String?,
    val descriptionHtml: String?,
    val artworkUrl: String?,
    /** True/false when the feed declares explicit content; null when unspecified. */
    val explicit: Boolean? = null,
    val categories: List<String> = emptyList(),
    /** Provider-owned category IDs keyed by their display names, when available. */
    val categoryIds: Map<String, String> = emptyMap(),
)

data class Episode(
    val id: Long,
    val podcastId: Long,
    val guid: String,
    val title: String,
    val descriptionHtml: String?,
    val audioUrl: String,
    val mimeType: String?,
    val artworkUrl: String?,
    val publishedAtMillis: Long?,
    val durationMs: Long?,
    val linkUrl: String? = null,
    val videoUrl: String? = null,
    val videoMimeType: String? = null,
    val audioSizeBytes: Long? = null,
    val videoSizeBytes: Long? = null,
    /** Episode-level value overrides the podcast value when declared. */
    val explicit: Boolean? = null,
)

data class AlternateEnclosure(
    val url: String,
    val mimeType: String,
    val title: String? = null,
    val height: Int? = null,
)

data class PodcastFeed(
    val podcast: Podcast,
    val episodes: List<Episode>,
    val etag: String? = null,
    val lastModified: String? = null,
    val notModified: Boolean = false,
)

data class PodcastSearchResult(
    val providerId: String,
    val podcast: Podcast,
)

enum class PodcastDiscoveryShelf(
    val id: String,
    val title: String,
) {
    TOP_SHOWS("top-shows", "Popular shows"),
}

data class PodcastDiscoveryCategory(
    val id: String,
    val title: String,
    val appleGenreId: String,
)

val PodcastDiscoveryCategories = listOf(
    PodcastDiscoveryCategory("arts", "Arts", "1301"),
    PodcastDiscoveryCategory("business", "Business", "1321"),
    PodcastDiscoveryCategory("comedy", "Comedy", "1303"),
    PodcastDiscoveryCategory("education", "Education", "1304"),
    PodcastDiscoveryCategory("fiction", "Fiction", "1483"),
    PodcastDiscoveryCategory("health", "Health & Fitness", "1512"),
    PodcastDiscoveryCategory("history", "History", "1487"),
    PodcastDiscoveryCategory("music", "Music", "1310"),
    PodcastDiscoveryCategory("news", "News", "1309"),
    PodcastDiscoveryCategory("science", "Science", "1533"),
    PodcastDiscoveryCategory("society", "Society & Culture", "1324"),
    PodcastDiscoveryCategory("sports", "Sports", "1545"),
    PodcastDiscoveryCategory("technology", "Technology", "1318"),
    PodcastDiscoveryCategory("true-crime", "True Crime", "1488"),
    PodcastDiscoveryCategory("tv-film", "TV & Film", "1311"),
)

data class PodcastDiscoveryShelfResult(
    val shelf: PodcastDiscoveryShelf,
    val results: List<PodcastSearchResult>,
    val title: String = shelf.title,
)

interface PodcastSearchProvider {
    val id: String
    val displayName: String
    /** Whether this provider can supply the unauthenticated popular-podcast shelf. */
    val supportsPopularPodcasts: Boolean get() = false
    suspend fun search(query: String): List<PodcastSearchResult>
    /** Gives a provider an opportunity to attach canonical metadata to a result before display. */
    suspend fun enrich(result: PodcastSearchResult): PodcastSearchResult = result
}

interface PodcastDiscoveryProvider {
    val id: String
    val supportsCategoryPaging: Boolean get() = false
    suspend fun browse(shelf: PodcastDiscoveryShelf): List<PodcastSearchResult>
    suspend fun browse(category: PodcastDiscoveryCategory): List<PodcastSearchResult>
    suspend fun browse(category: PodcastDiscoveryCategory, offset: Int): List<PodcastSearchResult> =
        if (offset == 0) browse(category) else emptyList()
}

interface PodcastDiscoveryCatalog {
    val shelves: List<PodcastDiscoveryShelf>
    val supportsCategoryPaging: Boolean
    suspend fun load(): List<PodcastDiscoveryShelfResult>
    suspend fun load(category: PodcastDiscoveryCategory): PodcastDiscoveryShelfResult
    suspend fun load(category: PodcastDiscoveryCategory, offset: Int): PodcastDiscoveryShelfResult
}

class DefaultPodcastDiscoveryCatalog(
    private val provider: PodcastDiscoveryProvider,
    override val shelves: List<PodcastDiscoveryShelf> = listOf(
        PodcastDiscoveryShelf.TOP_SHOWS,
    ),
) : PodcastDiscoveryCatalog {
    override val supportsCategoryPaging: Boolean = provider.supportsCategoryPaging
    override suspend fun load(): List<PodcastDiscoveryShelfResult> = shelves.map { shelf ->
        PodcastDiscoveryShelfResult(shelf, provider.browse(shelf))
    }

    override suspend fun load(category: PodcastDiscoveryCategory): PodcastDiscoveryShelfResult =
        load(category, 0)

    override suspend fun load(category: PodcastDiscoveryCategory, offset: Int): PodcastDiscoveryShelfResult =
        PodcastDiscoveryShelfResult(
            shelf = PodcastDiscoveryShelf.TOP_SHOWS,
            results = provider.browse(category, offset),
            title = category.title,
        )
}

interface PodcastSearchCatalog {
    val providers: List<PodcastSearchProvider>
    suspend fun search(providerId: String, query: String): List<PodcastSearchResult>
}

class DefaultPodcastSearchCatalog(
    override val providers: List<PodcastSearchProvider>,
) : PodcastSearchCatalog {
    private val providersById = providers.associateBy(PodcastSearchProvider::id)

    override suspend fun search(providerId: String, query: String): List<PodcastSearchResult> =
        (providersById[providerId] ?: providers.first()).search(query)
}

interface PodcastFeedProvider {
    suspend fun fetch(
        feedUrl: String,
        etag: String? = null,
        lastModified: String? = null,
        onEpisodeProgress: (processed: Int, total: Int) -> Unit = { _, _ -> },
    ): PodcastFeed
}

fun podcastId(feedUrl: String): Long = stableFeedId(canonicalFeedUrl(feedUrl))

fun episodeId(podcastId: Long, guid: String, audioUrl: String): Long =
    // RSS defines guid as the item's stable identity. Enclosure URLs can change when a
    // publisher moves or re-signs media, so they must not reset progress or downloads.
    stablePodcastId("$podcastId|${guid.trim().ifBlank { audioUrl.trim() }}")

private fun stablePodcastId(value: String): Long {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
    var result = 0L
    for (index in 0 until Long.SIZE_BYTES) {
        result = (result shl 8) or (digest[index].toLong() and 0xff)
    }
    return result and Long.MAX_VALUE
}
