package com.shapeshed.booth.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

/** Serializes memory-heavy feed refresh work across WorkManager workers in this app process. */
@Singleton
class PodcastRefreshCoordinator @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> withExclusiveRefresh(block: suspend () -> T): T {
        mutex.lock()
        try {
            return block()
        } finally {
            mutex.unlock()
        }
    }
}
