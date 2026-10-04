package com.shapeshed.booth.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val ACTION_SLEEP_TIMER_SET = "com.shapeshed.booth.action.SLEEP_TIMER_SET"
const val ACTION_SLEEP_TIMER_CANCEL = "com.shapeshed.booth.action.SLEEP_TIMER_CANCEL"
const val SLEEP_TIMER_DURATION_MS = "durationMs"

data class SleepTimerState(val totalMs: Long, val remainingMs: Long)

/**
 * A sleep-timer budget measured in *playback* time, not wall time.
 *
 * A 30 minute sleep timer means "stop after 30 minutes of audio", so paused time does not count
 * against it. Keeping that arithmetic here, with no player and no coroutines, is what makes the
 * rule testable: the service owns the clock and the pausing, this owns the subtraction.
 */
class SleepTimerBudget(val totalMs: Long) {
    var remainingMs: Long = totalMs
        private set

    val isExhausted: Boolean get() = remainingMs <= 0L

    /**
     * Consumes [elapsedMs] of playback time.
     *
     * @return true once the budget is spent, so the caller can stop playback exactly once.
     */
    fun consume(elapsedMs: Long): Boolean {
        if (isExhausted) return true
        remainingMs -= elapsedMs.coerceAtLeast(0L)
        return isExhausted
    }

    fun state(): SleepTimerState = SleepTimerState(totalMs, remainingMs.coerceAtLeast(0L))
}

/**
 * The timer's visible state, for the sheet and the notification.
 *
 * Bound as a `@Singleton` so it is visible in the Hilt object graph and replaceable in tests.
 * The service is the only writer; the authoritative remaining time lives in [SleepTimerBudget];
 * this is only what the UI observes.
 */
@Singleton
class SleepTimerStore @Inject constructor() {
    private val _state = MutableStateFlow<SleepTimerState?>(null)
    val state: StateFlow<SleepTimerState?> = _state.asStateFlow()

    fun set(value: SleepTimerState?) {
        _state.value = value
    }

    fun clear() {
        _state.value = null
    }
}
