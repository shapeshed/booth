package com.shapeshed.booth.data

/**
 * Whether the Inbox has episodes the user has not looked at yet.
 *
 * The Inbox dot used to mean "the Inbox is not empty", which has no clearing event: an episode only
 * leaves the Inbox when it is dismissed, queued or explicitly cleared, and listening to it does
 * nothing. So the dot could sit there indefinitely over episodes the user had already dealt with, and
 * it disagreed with the screen's own "New episodes from your subscriptions" wording.
 *
 * It now means "episodes arrived since you last opened the Inbox", which is what a badge on a tab is
 * normally taken to mean. It is a pure function of two timestamps so the rule can be tested without a
 * database or a settings store.
 *
 * [newestInboxFirstSeenAtMillis] is null when the Inbox is empty, and an empty Inbox is never unseen.
 * [lastInboxViewedAtMillis] of 0 is the never-opened case, so everything currently in the Inbox counts
 * as unseen and the dot shows on a fresh install.
 *
 * `>` rather than `>=`: opening the Inbox records the newest timestamp it was showing, and that
 * episode must not keep the dot alive after the user has just looked at it.
 */
internal fun hasUnseenInboxEpisodes(newestInboxFirstSeenAtMillis: Long?, lastInboxViewedAtMillis: Long): Boolean =
    newestInboxFirstSeenAtMillis != null && newestInboxFirstSeenAtMillis > lastInboxViewedAtMillis
