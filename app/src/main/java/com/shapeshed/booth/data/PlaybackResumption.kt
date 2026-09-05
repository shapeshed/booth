package com.shapeshed.booth.data

/** Returns the persisted playback item followed by the remaining queue in stored order. */
internal fun orderedResumptionIds(activeEpisodeId: Long, queuedEpisodeIds: List<Long>): List<Long> =
    buildList {
        add(activeEpisodeId)
        addAll(queuedEpisodeIds.filter { it != activeEpisodeId })
    }
