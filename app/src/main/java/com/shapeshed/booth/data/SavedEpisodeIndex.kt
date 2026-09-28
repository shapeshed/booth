package com.shapeshed.booth.data

/**
 * Resolves backup entries to episodes that are already saved, reading each podcast's episodes once.
 *
 * A backup carries one entry per known episode and a real library has thousands of them, so
 * resolving each entry with a fresh query made restore quadratic: the identity pass alone queried
 * twice per episode, and each query returned and then linearly scanned that podcast's whole episode
 * list. Restoring a real library took long enough to look like a hang, and it happened in a path
 * with no test coverage at all.
 *
 * Both the guid and the audio URL are indexed because a backup entry can legitimately match an
 * existing episode by either, which is what the predicate this replaces did. A key collision
 * between one episode's guid and another episode's audio URL is possible in principle and was
 * equally ambiguous before, so the behaviour is unchanged.
 *
 * Per podcast, and reset between imports by being a local.
 */
internal class SavedEpisodeIndex {
    private val byKey = mutableMapOf<Long, MutableMap<String, EpisodeEntity>>()
    private val loaded = mutableSetOf<Long>()

    /** The saved episode matching [guid] or [audioUrl] within [podcastId], or null. */
    suspend fun find(
        podcastId: Long,
        guid: String,
        audioUrl: String,
        loadEpisodes: suspend (Long) -> List<EpisodeEntity>,
    ): EpisodeEntity? {
        val index = byKey.getOrPut(podcastId) { mutableMapOf() }
        if (loaded.add(podcastId)) {
            loadEpisodes(podcastId).forEach { index.put(it) }
        }
        return index[guid] ?: index[audioUrl]
    }

    /**
     * Records an episode that has just been inserted, so a later lookup finds it.
     *
     * Deliberately does not mark the podcast as loaded. A subsequent [find] will still read from
     * the database, which is correct: there may be pre-existing episodes this index has not seen,
     * and the row just written is one of them.
     */
    fun put(episode: EpisodeEntity) {
        val index = byKey.getOrPut(episode.podcastId) { mutableMapOf() }
        index.put(episode)
    }

    private fun MutableMap<String, EpisodeEntity>.put(episode: EpisodeEntity) {
        this[episode.guid] = episode
        this[episode.audioUrl] = episode
    }
}
