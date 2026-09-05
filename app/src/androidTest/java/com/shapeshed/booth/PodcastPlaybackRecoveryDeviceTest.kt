package com.shapeshed.booth

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shapeshed.booth.data.Episode
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastPlaybackRecoveryDeviceTest {
    private lateinit var app: BoothApp
    private lateinit var server: WavServer
    private var episodeId = 0L

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        val address = deviceAddress()
        assumeTrue("No routable device address available", address != null)
        server = WavServer()
        episodeId = 8_000_000_000L + System.currentTimeMillis() / 1_000L
        runBlocking {
            app.podcastRepository.savePreviewEpisode(
                Episode(
                    id = episodeId,
                    podcastId = episodeId,
                    guid = "device-playback-fixture",
                    title = "Device playback fixture",
                    descriptionHtml = null,
                    audioUrl = server.url(requireNotNull(address)),
                    mimeType = "audio/wav",
                    artworkUrl = null,
                    publishedAtMillis = null,
                    durationMs = 10_000L,
                ),
            )
            app.settings.setPodcastLastEpisodeId(episodeId)
        }
    }

    @After
    fun tearDown() {
        runCatching {
            runBlocking {
                app.podcastRepository.removeEpisode(episodeId)
                app.settings.clearPodcastLastEpisodeId()
            }
        }
        runCatching { server.close() }
    }

    @Test
    fun serviceResumesFixtureAfterBeingStopped() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val token = SessionToken(context, ComponentName(context, PodcastPlaybackService::class.java))
        val firstFuture = MediaController.Builder(context, token).buildAsync()
        val first = firstFuture.get(10, TimeUnit.SECONDS)
        try {
            val item = MediaItem.Builder()
                .setMediaId(episodeId.toString())
                .setUri(server.url(requireNotNull(deviceAddress())))
                .setMimeType("audio/wav")
                .build()
            first.setMediaItem(item)
            first.prepare()
            first.play()
            waitUntil { first.playbackState == Player.STATE_READY && first.isPlaying }
            assertTrue(first.currentPosition >= 0L)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                first.release()
                MediaController.releaseFuture(firstFuture)
            }
        }

        context.stopService(android.content.Intent(context, PodcastPlaybackService::class.java))
        Thread.sleep(500L)

        val secondFuture = MediaController.Builder(context, token).buildAsync()
        val second = secondFuture.get(10, TimeUnit.SECONDS)
        try {
            waitUntil { second.currentMediaItem?.mediaId == episodeId.toString() }
            assertEquals(episodeId.toString(), second.currentMediaItem?.mediaId)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                second.release()
                MediaController.releaseFuture(secondFuture)
            }
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        repeat(100) {
            if (condition()) return
            Thread.sleep(100L)
        }
        assertTrue("Condition was not reached before timeout", condition())
    }

    private fun deviceAddress(): InetAddress? = Collections.list(NetworkInterface.getNetworkInterfaces())
        .asSequence()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { Collections.list(it.inetAddresses).asSequence() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull()

    private class WavServer : AutoCloseable {
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
                        val body = wavBody()
                        val header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: audio/wav\r\n" +
                            "Content-Length: ${body.size}\r\n" +
                            "Connection: close\r\n\r\n"
                        output.write(header.toByteArray())
                        output.write(body)
                        output.flush()
                    }
                }
            }
        }.apply { start() }

        fun url(address: InetAddress): String = "http://${address.hostAddress}:$port/fixture.wav"

        private val port: Int
            get() = socket.localPort

        override fun close() {
            if (closed.compareAndSet(false, true)) socket.close()
            thread.join(1_000L)
        }

        private fun wavBody(): ByteArray {
            val sampleRate = 8_000
            val samples = sampleRate * 10
            val dataSize = samples * 2
            val bytes = ByteArray(44 + dataSize)
            fun putAscii(offset: Int, value: String) {
                value.toByteArray().copyInto(bytes, offset)
            }
            fun putInt(offset: Int, value: Int) {
                for (index in 0 until 4) bytes[offset + index] = (value shr (index * 8)).toByte()
            }
            fun putShort(offset: Int, value: Int) {
                bytes[offset] = value.toByte()
                bytes[offset + 1] = (value shr 8).toByte()
            }
            putAscii(0, "RIFF")
            putInt(4, bytes.size - 8)
            putAscii(8, "WAVEfmt ")
            putInt(16, 16)
            putShort(20, 1)
            putShort(22, 1)
            putInt(24, sampleRate)
            putInt(28, sampleRate * 2)
            putShort(32, 2)
            putShort(34, 16)
            putAscii(36, "data")
            putInt(40, dataSize)
            return bytes
        }
    }
}
