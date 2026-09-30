package com.shapeshed.booth.data

const val ACTION_SKIP_SILENCE_SET = "com.shapeshed.booth.action.SKIP_SILENCE_SET"
const val SKIP_SILENCE_ENABLED = "skip_silence_enabled"

/**
 * How a per-podcast playback setting relates to the app-wide one.
 *
 * Both are nullable on the podcast, and null means "follow the global", so the global is a default
 * rather than a value every podcast has to agree with. This is the model AntennaPod uses, and the
 * shape the player wants: a feed can be deliberately slower than everything else without affecting
 * anything else, and reverting a podcast to the default is a single nullable write.
 */
object PlaybackSettings {
    const val DEFAULT_SPEED = 1f
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 3f

    /** Keeps a speed inside what the player and the slider can represent. */
    fun clampSpeed(speed: Float): Float = speed.coerceIn(MIN_SPEED, MAX_SPEED)

    /**
     * The speed an episode plays at: the podcast's own, or the app-wide default.
     *
     * A podcast set to exactly [DEFAULT_SPEED] is an explicit choice, not an absence, and is
     * returned as-is. Collapsing it into the global would mean a listener could not keep one feed at
     * normal speed while changing the default for everything else.
     */
    fun effectiveSpeed(podcastSpeed: Float?, globalSpeed: Float): Float = podcastSpeed ?: globalSpeed

    /**
     * Whether an episode skips silence: the podcast's own, or the app-wide default.
     *
     * `false` is a real answer, not a missing one. Treating it as absent would make it impossible to
     * keep silence in one feed while turning skipping on everywhere else, and it is the mistake that
     * makes nullable booleans dangerous in the first place.
     */
    fun effectiveSkipSilence(podcastSkipSilence: Boolean?, globalSkipSilence: Boolean): Boolean =
        podcastSkipSilence ?: globalSkipSilence
}
