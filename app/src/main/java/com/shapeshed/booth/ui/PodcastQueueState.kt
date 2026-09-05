package com.shapeshed.booth.ui

internal data class QueueRemoval<T>(
    val item: T,
    val originalIndex: Int,
)

internal fun <T> removeQueueItem(items: List<T>, item: T): Pair<List<T>, QueueRemoval<T>>? {
    val index = items.indexOf(item)
    if (index < 0) return null
    return items.filterIndexed { itemIndex, _ -> itemIndex != index } to QueueRemoval(item, index)
}

internal fun <T> restoreQueueItem(items: List<T>, removal: QueueRemoval<T>): List<T> =
    items.toMutableList().apply {
        add(removal.originalIndex.coerceIn(0, size), removal.item)
    }
