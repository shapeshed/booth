package com.shapeshed.booth.data

import android.app.DownloadManager

private const val HTTP_NOT_FOUND = 404
private const val HTTP_GONE = 410

/**
 * The wording used when the enclosure is gone for good.
 *
 * A named constant rather than an inline string because [isDownloadPermanentlyUnavailable] has to
 * recognise it again later. The UI only ever sees the stored message, not the platform reason, and the
 * entity has no column for the reason, so the message is the only durable carrier of "retrying cannot
 * help". Comparing against this one value keeps that decision in a single place instead of matching a
 * substring of rendered text, which would break silently the moment the wording changed.
 */
internal const val DOWNLOAD_UNAVAILABLE_MESSAGE = "The audio file is no longer available at this address."

/**
 * The wording used when an enqueue failed without giving a usable reason.
 *
 * Named rather than inline so the failure row and any test agree on one string.
 */
internal const val DOWNLOAD_FAILED_TO_START_MESSAGE = "The download could not be started."

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

    // DownloadManager reports ERROR_UNHANDLED_HTTP_CODE with COLUMN_REASON set to the HTTP status
    // itself, so these arrive here as bare status codes rather than as an ERROR_* constant. A 404 is
    // the common real-world case and is worth naming, because it is the one failure where retrying
    // cannot possibly help and the user should be told to remove the download instead. Observed on a
    // device: an FT News Briefing enclosure that had been pulled reported exactly "(code 404)".
    HTTP_NOT_FOUND, HTTP_GONE -> DOWNLOAD_UNAVAILABLE_MESSAGE

    DownloadManager.ERROR_UNKNOWN -> "The download failed for an unknown reason."

    else -> "The download failed (code $reason)."
}

/**
 * Whether the enclosure is gone and a retry is pointless.
 *
 * Takes the stored message rather than the platform reason because that is all the UI and the
 * persisted row have.
 */
internal fun isDownloadPermanentlyUnavailable(errorMessage: String?): Boolean =
    errorMessage == DOWNLOAD_UNAVAILABLE_MESSAGE
