package com.shapeshed.booth

import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PodcastPlaybackServiceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun mediaSessionServiceCanConnectAndRelease() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val future = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PodcastPlaybackService::class.java)),
        ).buildAsync()

        try {
            val controller = future.get(10, TimeUnit.SECONDS)
            assertNotNull(controller)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.release()
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                MediaController.releaseFuture(future)
            }
        }
    }
}
