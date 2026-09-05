package com.shapeshed.booth.data

/** Pure playback rules for configured intro and ending skips. */
object PlaybackSkipPolicy {
    fun introPosition(positionMs: Long, skipStartMs: Long, durationMs: Long): Long {
        if (skipStartMs <= 0L || positionMs >= skipStartMs) return positionMs
        if (durationMs > 0L && skipStartMs >= durationMs) return positionMs
        return skipStartMs
    }

    fun shouldSkipEnding(
        positionMs: Long,
        durationMs: Long,
        skipEndMs: Long,
        playbackSpeed: Float,
    ): Boolean {
        if (skipEndMs <= 0L || durationMs <= 0L) return false
        return skipEndMs < durationMs &&
            positionMs >= durationMs - skipEndMs
    }
}
