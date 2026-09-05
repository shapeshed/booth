package com.shapeshed.booth.data

import java.net.URI

/**
 * Pure text helpers (no Android APIs) so they can be unit-tested directly.
 */

/** Display host for a feed URL — the host with any leading `www.` removed, or null if unparseable. */
fun hostOf(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val host = runCatching { URI(url).host }.getOrNull() ?: return null
    return host.removePrefix("www.").takeIf { it.isNotBlank() }
}

/**
 * Short relative time like "just now", "5m", "3h", "2d", "4mo", "1y", given an item's unix
 * [epochSeconds] and the current time [nowSeconds].
 */
fun relativeTime(epochSeconds: Long, nowSeconds: Long): String {
    val diff = (nowSeconds - epochSeconds).coerceAtLeast(0)
    return when {
        diff < 60 -> "just now"
        diff < 3_600 -> "${diff / 60}m"
        diff < 86_400 -> "${diff / 3_600}h"
        diff < 2_592_000 -> "${diff / 86_400}d"
        diff < 31_536_000 -> "${diff / 2_592_000}mo"
        else -> "${diff / 31_536_000}y"
    }
}
