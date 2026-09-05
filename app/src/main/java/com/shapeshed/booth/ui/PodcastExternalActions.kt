package com.shapeshed.booth.ui

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

fun openExternally(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

fun shareLink(context: Context, title: String, url: String) {
    runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, title)
                    .putExtra(Intent.EXTRA_TEXT, url),
                null,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
