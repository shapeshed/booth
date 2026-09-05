package com.shapeshed.booth.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val ACTION_SLEEP_TIMER_SET = "com.shapeshed.booth.action.SLEEP_TIMER_SET"
const val ACTION_SLEEP_TIMER_CANCEL = "com.shapeshed.booth.action.SLEEP_TIMER_CANCEL"
const val SLEEP_TIMER_DURATION_MS = "durationMs"

data class SleepTimerState(val totalMs: Long, val remainingMs: Long)

object SleepTimerStore {
    private val _state = MutableStateFlow<SleepTimerState?>(null)
    val state: StateFlow<SleepTimerState?> = _state.asStateFlow()
    fun set(value: SleepTimerState?) { _state.value = value }
}
