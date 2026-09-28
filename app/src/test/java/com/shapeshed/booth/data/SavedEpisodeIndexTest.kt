package com.shapeshed.booth.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * [SavedEpisodeIndex] exists because backup restore was quadratic: it re-read and re-scanned a
 * podcast's whole episode list for every entry in the backup, twice per entry in the identity pass.
 * The count assertions here are the point of the class, not an incidental detail.
 */
class SavedEpisodeIndexTest {
    private fun episode(
        id: Long,
        podcastId: Long = 1L,
        guid: String = "guid-$id",
        audioUrl: String = "https://example.com/$id.mp3",
    ) = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        guid = guid,
        title = "Episode $id",
        descriptionHtml = null,
        audioUrl = audioUrl,
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = null,
        localUri = null,
        positionMs = 0L,
        completed = false,
        inInbox = false,
        firstSeenAtMillis = null,
    )

    /** Counts loads so the tests can assert the query count rather than infer it from a timeout. */
    private class Loader(private val episodes: List<EpisodeEntity>) {
        var calls = 0
            private set

        suspend operator fun invoke(podcastId: Long): List<EpisodeEntity> {
            calls++
            return episodes.filter { it.podcastId == podcastId }
        }
    }

    @Test
    fun matchesOnGuid() = runBlocking {
        val saved = episode(10L)
        val index = SavedEpisodeIndex()
        val loader = Loader(listOf(saved))

        val found = index.find(
            podcastId = saved.podcastId,
            guid = saved.guid,
            audioUrl = "https://other/x.mp3",
            loadEpisodes = loader::invoke,
        )

        assertSame(saved, found)
    }

    /** The original predicate matched either field, so a redirected enclosure still resolves. */
    @Test
    fun matchesOnAudioUrl() = runBlocking {
        val saved = episode(11L)
        val index = SavedEpisodeIndex()
        val loader = Loader(listOf(saved))

        val found = index.find(
            podcastId = saved.podcastId,
            guid = "not-the-guid",
            audioUrl = saved.audioUrl,
            loadEpisodes = loader::invoke,
        )

        assertSame(saved, found)
    }

    @Test
    fun returnsNullWhenNothingMatches() = runBlocking {
        val index = SavedEpisodeIndex()
        val loader = Loader(listOf(episode(12L)))

        val found = index.find(1L, guid = "nope", audioUrl = "https://nope.mp3", loadEpisodes = loader::invoke)

        assertNull(found)
    }

    @Test
    fun doesNotMatchAcrossPodcasts() = runBlocking {
        val other = episode(13L, podcastId = 2L)
        val index = SavedEpisodeIndex()
        val loader = Loader(listOf(episode(14L, podcastId = 1L), other))

        val found = index.find(1L, guid = other.guid, audioUrl = other.audioUrl, loadEpisodes = loader::invoke)

        assertNull(found)
    }

    /**
     * The regression. A library of 5,000 episodes in 20 podcasts, with a backup entry for each,
     * used to issue a query per entry, twice over in the identity pass, and scan the podcast's
     * episodes each time.
     */
    @Test
    fun readsEachPodcastsEpisodesOnceNoMatterHowManyEntries() = runBlocking {
        val saved = (1L..250L).map { episode(it) }
        val index = SavedEpisodeIndex()
        val loader = Loader(saved)

        repeat(250) { offset ->
            val target = saved[offset]
            index.find(target.podcastId, target.guid, target.audioUrl, loader::invoke)
        }

        assertEquals(
            "the database should be read once per podcast, not once per entry",
            1,
            loader.calls,
        )
    }

    /** Two podcasts in one backup means two reads, not two per entry. */
    @Test
    fun readsOnceForEachDistinctPodcast() = runBlocking {
        val saved = (1L..10L).map { episode(it, podcastId = if (it % 2 == 0L) 1L else 2L) }
        val index = SavedEpisodeIndex()
        val loader = Loader(saved)

        saved.forEach { index.find(it.podcastId, it.guid, it.audioUrl, loader::invoke) }

        assertEquals(2, loader.calls)
    }

    /** An episode inserted during the restore has to be findable by the passes that follow. */
    @Test
    fun findsAnEpisodeRecordedAfterTheLoad() = runBlocking {
        val index = SavedEpisodeIndex()
        val loader = Loader(emptyList())
        val inserted = episode(20L)

        index.put(inserted)
        val found = index.find(inserted.podcastId, inserted.guid, inserted.audioUrl, loader::invoke)

        assertSame(inserted, found)
    }

    /**
     * [SavedEpisodeIndex.put] must not mark the podcast loaded, or an episode written during the
     * restore would hide every pre-existing episode of that podcast.
     */
    @Test
    fun stillReadsTheDatabaseAfterAnEpisodeIsRecorded() = runBlocking {
        val preExisting = episode(30L)
        val index = SavedEpisodeIndex()
        val loader = Loader(listOf(preExisting))

        index.put(episode(31L))
        val found = index.find(preExisting.podcastId, preExisting.guid, preExisting.audioUrl, loader::invoke)

        assertEquals("the database should still have been read", 1, loader.calls)
        assertSame(preExisting, found)
    }
}
