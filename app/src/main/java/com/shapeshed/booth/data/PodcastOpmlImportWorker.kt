package com.shapeshed.booth.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.shapeshed.booth.BoothApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

const val OPML_IMPORT_FILE_INPUT = "opmlFile"
const val OPML_IMPORT_COMPLETED = "completed"
const val OPML_IMPORT_TOTAL = "total"
const val OPML_IMPORT_IMPORTED = "imported"

class PodcastOpmlImportWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val filePath = inputData.getString(OPML_IMPORT_FILE_INPUT)
            ?.takeIf(String::isNotBlank)
            ?: return@withContext Result.failure()
        try {
            val feeds = applicationContext.openFileInput(filePath).bufferedReader().use { reader ->
                parseOpmlFeeds(reader.readText())
            }
            if (feeds.isEmpty()) return@withContext Result.failure()

            val repository = (applicationContext as BoothApp).podcastRepository
            val imported = AtomicInteger(0)
            val completed = AtomicInteger(0)
            val semaphore = Semaphore(MAX_CONCURRENT_FEEDS)
            setProgress(workDataOf(OPML_IMPORT_COMPLETED to 0, OPML_IMPORT_TOTAL to feeds.size))
            coroutineScope {
                feeds.map { feed ->
                    async {
                        semaphore.withPermit {
                            runCatching { repository.subscribe(feed.url, feed.tags) }
                                .onSuccess { imported.incrementAndGet() }
                            setProgress(
                                workDataOf(
                                    OPML_IMPORT_COMPLETED to completed.incrementAndGet(),
                                    OPML_IMPORT_TOTAL to feeds.size,
                                ),
                            )
                        }
                    }
                }.awaitAll()
            }
            val result = Result.success(
                workDataOf(
                    OPML_IMPORT_COMPLETED to feeds.size,
                    OPML_IMPORT_TOTAL to feeds.size,
                    OPML_IMPORT_IMPORTED to imported.get(),
                ),
            )
            applicationContext.deleteFile(filePath)
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private companion object {
        const val MAX_CONCURRENT_FEEDS = 4
    }
}
