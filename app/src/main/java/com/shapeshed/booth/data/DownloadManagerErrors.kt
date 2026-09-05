package com.shapeshed.booth.data

import android.app.DownloadManager

/** Stable, user-safe diagnostics for the platform download service. */
internal fun downloadManagerFailureMessage(reason: Int): String = when (reason) {
    DownloadManager.ERROR_CANNOT_RESUME -> "The download could not resume."
    DownloadManager.ERROR_DEVICE_NOT_FOUND -> "The download storage is unavailable."
    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "The download file already exists."
    DownloadManager.ERROR_FILE_ERROR -> "The download file could not be written."
    DownloadManager.ERROR_HTTP_DATA_ERROR -> "The server connection failed while downloading."
    DownloadManager.ERROR_INSUFFICIENT_SPACE -> "There is not enough storage for this download."
    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "The media URL redirected too many times."
    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "The server returned an unsupported response."
    DownloadManager.ERROR_UNKNOWN -> "The download failed for an unknown reason."
    else -> "The download failed (code $reason)."
}
