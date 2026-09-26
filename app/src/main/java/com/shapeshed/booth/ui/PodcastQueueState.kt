package com.shapeshed.booth.ui

internal data class QueueRemoval<T>(val item: T, val originalIndex: Int)

internal fun <T> removeQueueItem(items: List<T>, item: T): Pair<List<T>, QueueRemoval<T>>? {
    val index = items.indexOf(item)
    if (index < 0) return null
    return items.filterIndexed { itemIndex, _ -> itemIndex != index } to QueueRemoval(item, index)
}

internal fun <T> restoreQueueItem(items: List<T>, removal: QueueRemoval<T>): List<T> = items.toMutableList().apply {
    add(removal.originalIndex.coerceIn(0, size), removal.item)
}

internal fun <T> reorderQueueItem(items: List<T>, item: T, target: T): List<T> {
    val itemIndex = items.indexOf(item)
    val targetIndex = items.indexOf(target)
    if (itemIndex < 0 || targetIndex < 0 || itemIndex == targetIndex) return items

    return items.toMutableList().apply {
        removeAt(itemIndex)
        add(targetIndex.coerceIn(0, size), item)
    }
}
