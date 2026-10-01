package com.shapeshed.booth

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That a headset's media buttons have somewhere to arrive. Issue #38.
 *
 * The system delivers hardware media buttons as `ACTION_MEDIA_BUTTON` broadcasts. Media3 receives
 * them through its own `MediaButtonReceiver`, which forwards them to the playback service, and the
 * service's own intent filters match the session and browser actions rather than the button press.
 * With that receiver absent, a Bluetooth headset's fast-forward and rewind did nothing: the
 * broadcast had no receiver at all, which is indistinguishable from a player that ignored the
 * command.
 *
 * The receiver is named by string rather than by class because it lives in the Media3 library, and
 * loading it here would fail the test for the wrong reason if Media3 were ever removed.
 */
@RunWith(AndroidJUnit4::class)
class MediaButtonReceiverManifestDeviceTest {
    @Test
    fun aMediaButtonBroadcastHasAReceiverInThisApp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val resolved = context.packageManager.queryBroadcastReceivers(
            Intent(Intent.ACTION_MEDIA_BUTTON),
            0,
        )

        assertTrue(
            "no receiver in ${context.packageName} handles ACTION_MEDIA_BUTTON, so hardware media " +
                "buttons cannot reach the playback service",
            resolved.any { it.activityInfo.packageName == context.packageName },
        )
    }

    @Test
    fun theMedia3ReceiverIsDeclaredAndExported() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Exported because the system's media button broadcast comes from outside the app. Without
        // it the receiver is unreachable however the filter is written.
        val receiver = context.packageManager.getReceiverInfo(
            ComponentName(context.packageName, "androidx.media3.session.MediaButtonReceiver"),
            0,
        )

        assertNotNull(receiver)
        assertTrue(receiver.exported)
    }
}
