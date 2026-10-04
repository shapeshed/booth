package com.shapeshed.booth.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Inbox badge rule.
 *
 * The dot used to mean "the Inbox is not empty", which never cleared on its own: episodes only leave
 * the Inbox when they are dismissed, queued or explicitly cleared, so listening to one left the dot
 * in place. It now means "arrived since the Inbox was last opened", and this pins the boundary that
 * makes it clear when the user looks.
 */
class InboxBadgeTest {

    @Test
    fun `an empty inbox is never unseen`() {
        assertFalse(hasUnseenInboxEpisodes(newestInboxFirstSeenAtMillis = null, lastInboxViewedAtMillis = 0L))
        assertFalse(
            hasUnseenInboxEpisodes(newestInboxFirstSeenAtMillis = null, lastInboxViewedAtMillis = 1_000L),
        )
    }

    @Test
    fun `never having opened the inbox makes everything unseen`() {
        assertTrue(hasUnseenInboxEpisodes(newestInboxFirstSeenAtMillis = 1L, lastInboxViewedAtMillis = 0L))
    }

    @Test
    fun `an arrival after the last visit is unseen`() {
        assertTrue(
            hasUnseenInboxEpisodes(newestInboxFirstSeenAtMillis = 2_000L, lastInboxViewedAtMillis = 1_000L),
        )
    }

    /**
     * The boundary that matters. Opening the Inbox records the newest timestamp it was showing, so
     * that episode must not keep the dot alive immediately afterwards.
     */
    @Test
    fun `the episode the marker was set from is not itself unseen`() {
        assertFalse(
            hasUnseenInboxEpisodes(newestInboxFirstSeenAtMillis = 1_000L, lastInboxViewedAtMillis = 1_000L),
        )
    }

    @Test
    fun `nothing newer than the last visit is unseen`() {
        assertFalse(
            hasUnseenInboxEpisodes(newestInboxFirstSeenAtMillis = 500L, lastInboxViewedAtMillis = 1_000L),
        )
    }

    /**
     * Only the newest arrival is compared. If it is not newer than the marker then none are, which is
     * what lets the query use MAX rather than counting rows.
     */
    @Test
    fun `the newest arrival decides for the whole inbox`() {
        // A marker sitting between two episodes: the newest is still newer, so the dot shows.
        assertTrue(
            hasUnseenInboxEpisodes(newestInboxFirstSeenAtMillis = 3_000L, lastInboxViewedAtMillis = 2_000L),
        )
    }

    /**
     * A row with no `firstSeenAtMillis` cannot raise the dot on its own.
     *
     * `MAX` in the query ignores NULLs, so an Inbox made only of such rows reads as null, the same as
     * an empty one. That is deliberate: substituting a default would either make an old episode look
     * like a new arrival or make it permanently invisible, and neither is worth a badge.
     */
    @Test
    fun `a row with no first-seen time cannot raise the dot by itself`() {
        assertFalse(
            hasUnseenInboxEpisodes(newestInboxFirstSeenAtMillis = null, lastInboxViewedAtMillis = 1_000L),
        )
    }
}
