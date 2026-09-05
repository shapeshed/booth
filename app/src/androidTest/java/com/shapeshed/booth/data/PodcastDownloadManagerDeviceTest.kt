package com.shapeshed.booth.data

import android.app.DownloadManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastDownloadManagerDeviceTest {
    private lateinit var database: PodcastDatabase
    private lateinit var repository: PodcastRepository
    private lateinit var manager: PodcastDownloadManager
    private lateinit var server: SlowDownloadServer
    private lateinit var episode: EpisodeEntity

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val address = deviceAddress()
        assumeTrue("No routable device address available", address != null)
        val routableAddress = requireNotNull(address)
        server = SlowDownloadServer()
        database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java).build()
        repository = PodcastRepository(
            feedProvider = object : PodcastFeedProvider {
                override suspend fun fetch(
                    feedUrl: String,
                    etag: String?,
                    lastModified: String?,
                    onEpisodeProgress: (processed: Int, total: Int) -> Unit,
                ): PodcastFeed = error("Not used by this test")
            },
            dao = database.podcastDao(),
            downloadDao = database.downloadAssetDao(),
        )
        manager = PodcastDownloadManager(context, repository)
        episode = EpisodeEntity(
            id = 9_876_543_210L,
            podcastId = 1L,
            guid = "device-download-fixture",
            title = "Device download fixture",
            descriptionHtml = null,
            audioUrl = server.url(routableAddress),
            mimeType = "audio/mpeg",
            artworkUrl = null,
            publishedAtMillis = null,
            durationMs = null,
            positionMs = 0L,
            completed = false,
            localUri = null,
            inInbox = false,
            firstSeenAtMillis = null,
        )
    }

    @After
    fun tearDown() {
        runCatching {
            runBlocking { manager.removeEpisodeDownloads(episode.id) }
        }
        runCatching { server.close() }
        runCatching { database.close() }
    }

    @Test
    fun cancellingSystemTransferReconcilesAsFailure() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager.enqueue(
            episode = episode,
            assetType = DownloadAssetType.AUDIO,
            url = episode.audioUrl,
            mimeType = episode.mimeType,
            expectedBytes = SlowDownloadServer.BODY_BYTES.toLong(),
        )
        val asset = repository.downloadAsset(episode.id, DownloadAssetType.AUDIO)
            ?: error("Download asset was not persisted")

        context.getSystemService(DownloadManager::class.java).remove(asset.downloadId)
        delay(300L)
        manager.syncActiveDownloads()

        assertEquals(
            DownloadAssetStatus.FAILED,
            repository.downloadAsset(episode.id, DownloadAssetType.AUDIO)?.status,
        )
    }

    private fun deviceAddress(): InetAddress? = Collections.list(NetworkInterface.getNetworkInterfaces())
        .asSequence()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { Collections.list(it.inetAddresses).asSequence() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull()

    private class SlowDownloadServer : AutoCloseable {
        private val closed = AtomicBoolean(false)
        private val socket = ServerSocket(0, 1, InetAddress.getByName("0.0.0.0"))
        private val thread = Thread {
            runCatching {
                socket.accept().use { connection ->
                    BufferedInputStream(connection.getInputStream()).use { input ->
                        while (input.read() != -1) {
                            if (input.available() == 0) break
                        }
                    }
                    BufferedOutputStream(connection.getOutputStream()).use { output ->
                        val header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: audio/mpeg\r\n" +
                            "Content-Length: $BODY_BYTES\r\n" +
                            "Connection: close\r\n\r\n"
                        output.write(header.toByteArray())
                        output.flush()
                        repeat(BODY_BYTES / CHUNK_BYTES) {
                            output.write(ByteArray(CHUNK_BYTES))
                            output.flush()
                            Thread.sleep(30L)
                        }
                    }
                }
            }
        }.apply { start() }

        fun url(address: InetAddress): String = "http://${address.hostAddress}:$port/file.mp3"

        private val port: Int
            get() = socket.localPort

        override fun close() {
            if (closed.compareAndSet(false, true)) socket.close()
            thread.join(1_000L)
        }

        companion object {
            const val BODY_BYTES = 64 * 1024
            private const val CHUNK_BYTES = 1 * 1024
        }
    }
}
