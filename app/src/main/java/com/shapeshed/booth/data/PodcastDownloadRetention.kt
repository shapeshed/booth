package com.shapeshed.booth.data

/** Pure selection rules for reclaiming space before automatic downloads. */
internal fun downloadsEligibleForDeletion(
    episodes: List<EpisodeEntity>,
    queuedEpisodeIds: Set<Long>,
    mode: PodcastDeleteBeforeAutoDownload = PodcastDeleteBeforeAutoDownload.ALL_ELIGIBLE,
): List<EpisodeEntity> = episodes
    .asSequence()
    // An episode can be downloaded as audio, video, or both.  Retention operates on
    // the episode as a whole because removal must keep the two assets in sync.
    .filter { it.localUri != null || it.localVideoUri != null }
    .filterNot { it.id in queuedEpisodeIds }
    .filterNot(EpisodeEntity::favorite)
    .filter { mode != PodcastDeleteBeforeAutoDownload.PLAYED || it.completed }
    .sortedBy { it.publishedAtMillis ?: Long.MIN_VALUE }
    .toList()
