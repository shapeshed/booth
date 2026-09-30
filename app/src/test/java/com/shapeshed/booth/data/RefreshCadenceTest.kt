package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-feed refresh policy.
 *
 * The case that shaped this is a daily news briefing. Under a single global interval it is either
 * polled far more often than it publishes, or polled late enough to miss the morning edition, and
 * those are the same knob. So the tests below are mostly about a daily feed, a weekly feed, and a
 * feed with no usable history being scheduled differently from each other.
 */
class RefreshCadenceTest {
    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = 1_700_000_000_000L

    /** A feed publishing every [interval], newest episode [ageAgo] ago. */
    private fun dailyFeed(interval: Long, episodes: Int = 10, newestAgo: Long = hour): List<Long> =
        (0 until episodes).map { now - newestAgo - it * interval }

    @Test
    fun aDailyFeedIsCheckedAboutADayApart() {
        val feed = dailyFeed(day)
        val lastRefresh = now - 2 * hour
        val due = RefreshCadence.nextDueMillis(lastRefresh, feed, now)
        val untilDue = due - now
        assertTrue("expected it to be due within a day, was $untilDue", untilDue in 0..(day.toInt()))
    }

    @Test
    fun theWindowOpensBeforeTheEpisodeIsExpected() {
        // Feeds publish late, so waiting for the expected moment and then fetching means the episode
        // is still not there. The window opens a quarter of a cadence early.
        val feed = dailyFeed(day)
        val newest = now - hour
        val expectedNext = newest + day
        val dueFrom = RefreshCadence.nextDueMillis(now - 2 * hour, feed, now)

        assertTrue(
            "the window must open before the episode is expected",
            dueFrom < expectedNext,
        )
        val lead = expectedNext - dueFrom
        // A quarter of a day, give or take a minute of clock arithmetic.
        assertTrue("expected a ~6h lead, got ${lead / hour}h", lead in (5 * hour)..(7 * hour))
    }

    @Test
    fun withoutTheEarlyWindowAFeedIsOnlyFetchedAfterItIsAlreadyLate() {
        // Guards the assertion above: if the lead were dropped, dueFrom would equal expectedNext and
        // a daily briefing would not be looked for until it had already missed its slot.
        val feed = dailyFeed(day)
        val newest = now - hour
        assertTrue(RefreshCadence.nextDueMillis(now - 2 * hour, feed, now) < newest + day)
    }

    @Test
    fun aWeeklyFeedIsNotPolledDaily() {
        val week = 7 * day
        val feed = dailyFeed(week)
        val lastRefresh = now - day
        val due = RefreshCadence.nextDueMillis(lastRefresh, feed, now)
        // Six days after a refresh, a weekly show is not due.
        assertFalse(RefreshCadence.isDue(lastRefresh, feed, now))
        // Just before its expected slot it becomes due.
        val justBeforeDue = due - 1
        assertFalse(RefreshCadence.isDue(lastRefresh, feed, justBeforeDue))
        assertTrue(RefreshCadence.isDue(lastRefresh, feed, due))
    }

    @Test
    fun aDailyAndAWeeklyFeedGetDifferentNextTimes() {
        val lastRefresh = now - 2 * hour
        val daily = RefreshCadence.nextDueMillis(lastRefresh, dailyFeed(day), now)
        val weekly = RefreshCadence.nextDueMillis(lastRefresh, dailyFeed(7 * day), now)
        // This is the whole point: one global interval could not produce these two different answers.
        assertTrue("daily should be due before weekly", daily < weekly)
    }

    @Test
    fun aFeedIsNotRefetchedImmediatelyAfterTheLastAttempt() {
        val feed = dailyFeed(day)
        val justRefreshed = now - 60_000L
        assertFalse(RefreshCadence.isDue(justRefreshed, feed, now))
    }

    @Test
    fun aFeedWithTooLittleHistoryFallsBackToAFixedInterval() {
        // Two episodes, no gap to learn from.
        val sparse = listOf(now - hour, now - 3 * day)
        val lastRefresh = now - hour
        assertEquals(
            lastRefresh + RefreshCadence.MINIMUM_GAP_MILLIS + RefreshCadence.UNKNOWN_PATTERN_INTERVAL_MILLIS,
            RefreshCadence.nextDueMillis(lastRefresh, sparse, now),
        )
        assertFalse(RefreshCadence.isDue(lastRefresh, sparse, now))
        assertTrue(
            RefreshCadence.isDue(
                lastRefresh,
                sparse,
                now + RefreshCadence.UNKNOWN_PATTERN_INTERVAL_MILLIS,
            ),
        )
    }

    @Test
    fun aFeedWithNoDatedEpisodesAtAllStillGetsChecked() {
        val lastRefresh = now - 2 * hour
        val due = RefreshCadence.nextDueMillis(lastRefresh, emptyList(), now)
        assertTrue("a feed with no dates must not be stranded", due > now)
    }

    @Test
    fun aPausedFeedIsCheckedDailyRatherThanOnItsOldCadence() {
        // A daily feed that stopped three weeks ago. Without the backoff it would still be "due"
        // every day because its pattern says daily, and that is the polling this avoids.
        val pausedFeed = (0 until 10).map { now - 21 * day - it * day }
        val lastRefresh = now - 6 * hour
        assertFalse(RefreshCadence.isDue(lastRefresh, pausedFeed, now))
        assertTrue(
            "should be due once a day",
            RefreshCadence.isDue(lastRefresh, pausedFeed, now + RefreshCadence.QUIET_INTERVAL_MILLIS),
        )
    }

    @Test
    fun anEndOfEpisodeCatchUpIsNotPostponed() {
        // Overdue stays overdue. If a fetch found nothing new, the next attempt is still due
        // immediately rather than being pushed out a whole interval, which is how a feed that
        // publishes late would otherwise be missed for a day.
        val feed = dailyFeed(day, newestAgo = 2 * day)
        val lastRefresh = now - 3 * hour
        assertTrue(RefreshCadence.isDue(lastRefresh, feed, now))
    }

    @Test
    fun oneLongHiatusDoesNotStretchTheCadence() {
        // A daily feed that skipped a fortnight once. The median must ignore that gap, so the feed
        // keeps its daily rhythm instead of being treated as fortnightly.
        val times = buildList {
            for (i in 0 until 8) add(now - i * day)
            add(now - 8 * day - 15 * day)
            add(now - 9 * day - 16 * day)
        }
        assertEquals(day, RefreshCadence.typicalIntervalMillis(times))
    }

    @Test
    fun cadenceIsTheMedianNotTheMean() {
        val times = listOf(
            now - day,
            now - 2 * day,
            now - 3 * day,
            now - 4 * day,
            now - 104 * day, // one enormous gap
        )
        val meanish = (now - times.last()) / 5
        val interval = RefreshCadence.typicalIntervalMillis(times)!!
        assertEquals(day, interval)
        assertTrue("median should be far below the mean", interval < meanish)
    }

    @Test
    fun aVeryFastFeedIsStillFloored() {
        // Ten-a-day feed. The early window must not exceed the cadence, or the feed is due
        // permanently and gets fetched on every pass.
        val fast = (0 until 12).map { now - it * 5 * 60_000L }
        val interval = RefreshCadence.typicalIntervalMillis(fast)!!
        val lastRefresh = now - hour
        val due = RefreshCadence.nextDueMillis(lastRefresh, fast, now)
        val untilDue = due - now
        assertTrue("window $untilDue should not exceed the cadence $interval", untilDue <= interval)
    }

    @Test
    fun futureOrZeroTimestampsAreIgnored() {
        val feed = listOf(0L, -5L, now, now - day, now - 2 * day)
        assertEquals(day, RefreshCadence.typicalIntervalMillis(feed))
    }

    @Test
    fun orderingOfTheInputDoesNotMatter() {
        val feed = dailyFeed(day)
        assertEquals(
            RefreshCadence.typicalIntervalMillis(feed),
            RefreshCadence.typicalIntervalMillis(feed.shuffled()),
        )
    }
}
