package com.shapeshed.booth.data

/**
 * When each podcast is next worth fetching.
 *
 * The app used to ask the listener how often to refresh, and applied one answer to every feed. That
 * cannot be right for a real library. A daily news briefing and a weekly magazine share a single
 * global setting, and whichever value suits one is wrong for the other: six-hourly polling is
 * wasteful for the weekly and still misses a briefing published before the listener wakes.
 *
 * So the cadence comes from the feed. The gap between recent episodes is the feed saying how often
 * it publishes, and the next one is expected roughly that long after the last. Each podcast is
 * scheduled individually for that moment rather than polled on a shared timer, so a weekly show
 * wakes the app once a week and the daily briefing wakes it once a day.
 *
 * Pure, so the policy is testable without a device, a clock, or a database.
 */
object RefreshCadence {
    /** Below this, two fetches of one feed are pointless. Also the floor for any derived cadence. */
    const val MINIMUM_GAP_MILLIS = 20L * 60 * 1000

    /** A feed with a known pattern is due this fraction of a cadence before it is expected. */
    private const val EARLY_FRACTION = 0.25f

    /** Never open the window earlier than this, or a fast feed would be due constantly. */
    private const val MINIMUM_EARLY_MILLIS = 60L * 60 * 1000

    /** Used when there are too few dated episodes to infer a pattern. */
    const val UNKNOWN_PATTERN_INTERVAL_MILLIS = 12L * 60 * 60 * 1000

    /** A feed silent this long is treated as paused rather than active. */
    const val QUIET_AFTER_MILLIS = 7L * 24 * 60 * 60 * 1000

    /** A paused feed is checked daily: enough to notice it resumed, not enough to cost anything. */
    const val QUIET_INTERVAL_MILLIS = 24L * 60 * 60 * 1000

    /** How many recent gaps to consider. Older episodes describe a schedule the feed has left. */
    private const val GAPS_CONSIDERED = 8

    /**
     * The typical gap between recent episodes, or null when there is not enough history.
     *
     * The median, not the mean, because one double release or one long hiatus should not move the
     * cadence: a daily feed that once skipped a fortnight still refreshes daily.
     */
    fun typicalIntervalMillis(publishedAtMillis: List<Long>): Long? {
        val times = publishedAtMillis.filter { it > 0 }.sortedDescending()
        if (times.size < 3) return null
        val gaps = times.zipWithNext { newer, older -> newer - older }
            .filter { it > 0 }
            .take(GAPS_CONSIDERED)
        if (gaps.size < 2) return null
        return gaps.sorted()[gaps.size / 2].coerceAtLeast(MINIMUM_GAP_MILLIS)
    }

    /**
     * When this podcast should next be fetched, as an absolute time.
     *
     * Never earlier than [MINIMUM_GAP_MILLIS] after the last attempt, so a feed that is already
     * being retried is not requeued on top of itself. Otherwise the answer is the moment the next
     * episode is expected, minus a little slack, because feeds publish late and often on their own
     * schedule rather than the one they announce.
     *
     * Once this time passes the feed stays due until it is actually fetched, so a late episode is
     * picked up on the next pass instead of waiting out another whole interval.
     */
    fun nextDueMillis(lastRefreshMillis: Long?, publishedAtMillis: List<Long>, nowMillis: Long): Long {
        val notBefore = (lastRefreshMillis ?: 0L) + MINIMUM_GAP_MILLIS

        val dated = publishedAtMillis.filter { it > 0 }
        val newestPublish = dated.maxOrNull()
            ?: return notBefore + UNKNOWN_PATTERN_INTERVAL_MILLIS

        // A feed that has been silent for a week is on hiatus or has ended. It still gets checked,
        // because a feed returning from hiatus is exactly the one worth noticing, but daily is
        // enough and polling a finished feed on a news cadence is the waste this avoids.
        if (nowMillis - newestPublish > QUIET_AFTER_MILLIS) {
            return notBefore + QUIET_INTERVAL_MILLIS
        }

        val interval = typicalIntervalMillis(dated)
            ?: return notBefore + UNKNOWN_PATTERN_INTERVAL_MILLIS

        val earlyBy = (interval * EARLY_FRACTION).toLong().coerceAtLeast(MINIMUM_EARLY_MILLIS)
        return maxOf(newestPublish + interval - earlyBy, notBefore)
    }

    /** Whether this podcast is due now, which is just [nextDueMillis] having passed. */
    fun isDue(lastRefreshMillis: Long?, publishedAtMillis: List<Long>, nowMillis: Long): Boolean =
        nowMillis >= nextDueMillis(lastRefreshMillis, publishedAtMillis, nowMillis)

    /**
     * How often the reconciliation pass runs.
     *
     * Not a cadence: this does no fetching, it only notices subscriptions that have lost their
     * scheduled refresh, such as after a subscribe, a reboot that pruned pending work, or a clock
     * change. WorkManager will not run a one-time request for a month, so feeds with long cadences
     * need something to re-arm them.
     */
    const val RECONCILE_INTERVAL_MILLIS = 12L * 60 * 60 * 1000
}
