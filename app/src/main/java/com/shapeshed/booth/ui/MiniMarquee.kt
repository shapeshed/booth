package com.shapeshed.booth.ui

import android.provider.Settings
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Aerial-style marquee that respects Android's reduced-animation accessibility setting. */
@Composable
fun Modifier.safeMarquee(): Modifier {
    val context = LocalContext.current
    val animationsRemoved = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    return if (animationsRemoved) this else this.basicMarquee()
}
