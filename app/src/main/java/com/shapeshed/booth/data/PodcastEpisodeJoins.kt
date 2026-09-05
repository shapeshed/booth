package com.shapeshed.booth.data

internal fun orderedQueueEpisodes(
    entries: List<QueueEntity>,
    episodesById: Map<Long, EpisodeEntity>,
): List<EpisodeEntity> = entries.mapNotNull { entry -> episodesById[entry.episodeId] }

internal fun downloadEpisodeIds(assets: List<DownloadAssetEntity>): List<Long> =
    assets.map(DownloadAssetEntity::episodeId).distinct()
