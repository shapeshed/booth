package com.shapeshed.booth.data

internal fun RssParseResult.toPodcastFeed(
    onEpisodeProgress: (processed: Int, total: Int) -> Unit = { _, _ -> },
): PodcastFeed {
    val podcast = Podcast(
        id = podcastId(feed.url),
        title = feed.title,
        author = feed.author,
        feedUrl = canonicalFeedUrl(feed.url),
        siteUrl = feed.siteUrl,
        descriptionHtml = feed.description,
        artworkUrl = feed.imageUrl,
        explicit = feed.explicit,
    )
    val episodes = entries.mapIndexedNotNull { index, entry ->
        val audioUrl = entry.audioUrl
        if (audioUrl.isNullOrBlank()) return@mapIndexedNotNull null
        onEpisodeProgress(index + 1, entries.size)
        Episode(
            id = episodeId(podcast.id, entry.id.toString(), audioUrl),
            podcastId = podcast.id,
            guid = entry.id.toString(),
            title = entry.title,
            descriptionHtml = entry.summaryHtml,
            audioUrl = audioUrl,
            mimeType = entry.audioMimeType,
            artworkUrl = entry.imageUrl ?: podcast.artworkUrl,
            publishedAtMillis = entry.publishedAtMillis,
            durationMs = entry.durationMs,
            linkUrl = entry.webUrl,
            videoUrl = entry.videoUrl ?: audioUrl.takeIf { entry.audioMimeType.orEmpty().startsWith("video/") },
            videoMimeType = entry.videoMimeType ?: entry.audioMimeType?.takeIf { it.startsWith("video/") },
            audioSizeBytes = entry.audioSizeBytes,
            videoSizeBytes = entry.videoSizeBytes
                ?: entry.audioSizeBytes.takeIf { entry.audioMimeType.orEmpty().startsWith("video/") },
            explicit = entry.explicit,
        )
    }
    return PodcastFeed(
        podcast = podcast,
        episodes = episodes,
        etag = etag,
        lastModified = lastModified,
        notModified = notModified,
    )
}
