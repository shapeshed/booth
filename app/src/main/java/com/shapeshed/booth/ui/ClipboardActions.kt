package com.shapeshed.booth.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Writes a short piece of text to the system clipboard.
 *
 * Kept out of the composables so they stay side-effect free and so the "copy the version label"
 * action can be asserted in a UI test. Reading the clipboard back is not an option under test:
 * since Android 10 `ClipboardManager.getPrimaryClip()` returns null unless the reading app holds
 * focus, which a `createComposeRule()` instrumentation test never does.
 */
internal fun copyTextToClipboard(context: Context, text: String, clipLabel: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(clipLabel, text))
}
