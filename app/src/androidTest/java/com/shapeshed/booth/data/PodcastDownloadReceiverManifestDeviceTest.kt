package com.shapeshed.booth.data

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastDownloadReceiverManifestDeviceTest {
    @Test
    fun completionReceiverAcceptsOnlyPlatformDownloadCompletionSenders() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = context.packageManager.getReceiverInfo(
            ComponentName(context, PodcastDownloadReceiver::class.java),
            0,
        )

        assertTrue(receiver.exported)
        assertEquals(
            "android.permission.SEND_DOWNLOAD_COMPLETED_INTENTS",
            receiver.permission,
        )
    }
}
