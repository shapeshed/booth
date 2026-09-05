package com.shapeshed.booth.data

/** A subscribed RSS/Atom source. */
data class RssFeed(
    val id: Long,
    val title: String,
    val url: String,
    val siteUrl: String?,
    val description: String? = null,
    val imageUrl: String? = null,
    val iconUrl: String? = null,
    val author: String? = null,
    val explicit: Boolean? = null,
)

/** A normalized RSS/Atom entry ready for podcast episode mapping. */
data class RssEntry(
    val id: Long,
    val feedId: Long,
    val feedTitle: String,
    val title: String,
    val feedOrder: Int = 0,
    val url: String,
    val author: String?,
    val publishedAtMillis: Long?,
    val summaryHtml: String?,
    val imageUrl: String?,
    val imageSource: RssImageSource,
    val audioUrl: String? = null,
    val audioMimeType: String? = null,
    val audioSizeBytes: Long? = null,
    val videoUrl: String? = null,
    val videoMimeType: String? = null,
    val videoSizeBytes: Long? = null,
    val webUrl: String? = null,
    val durationMs: Long? = null,
    val explicit: Boolean? = null,
)

enum class RssImageSource {
    NONE,
    BODY,
    OG,
    FEED,
}
