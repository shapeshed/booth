package com.shapeshed.booth.ui

import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.searchableCategories

internal fun podcastIdsForEpisodeTags(
    podcasts: List<PodcastEntity>,
    selectedTags: Set<String>,
): List<Long>? = selectedTags
    .takeIf { it.isNotEmpty() }
    ?.let { tags ->
        podcasts.filter { podcast ->
            podcast.searchableCategories().any(tags::contains)
        }.map(PodcastEntity::id)
    }
