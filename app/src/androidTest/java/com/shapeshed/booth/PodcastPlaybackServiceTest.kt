package com.shapeshed.booth

import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastPlaybackServiceTest {
    @Test
    fun mediaSessionServiceCanConnectAndRelease() {
        val context = ApplicationProvider.getApplicationContext<BoothApp>()
        val future = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PodcastPlaybackService::class.java)),
        ).buildAsync()

        try {
            val controller = future.get(10, TimeUnit.SECONDS)
            assertNotNull(controller)
            controller.release()
        } finally {
            MediaController.releaseFuture(future)
        }
    }
}
