package com.shapeshed.booth.data

/**
 * The one download affordance an episode row can show.
 *
 * Up next, Inbox and Downloads each used to work this out for themselves. The three disagreed: a
 * failed download drew its icon inline in the metadata line on Downloads and drew nothing at all in
 * the other two, so the same episode looked broken in one list and merely undownloaded in the
 * others. Deriving it once here is what makes the badge mean the same thing everywhere.
 *
 * [NONE] means the row should show no download indicator, rather than an empty or zero-width one.
 */
enum class DownloadBadge { NONE, DOWNLOADED, WAITING_FOR_WIFI, DOWNLOADING, RETRYING, FAILED, CANCELLED }

/**
 * Works out the badge for one episode from its durable download row and its live progress.
 *
 * [isDownloaded] is the caller's existing "there is a playable file or the transfer just finished"
 * answer, kept as an argument so this stays a pure function of its inputs.
 *
 * Precedence, and why:
 *
 * 1. A file that is actually there wins over everything. If bytes are on disk and playable, the
 *    episode is available offline whatever the row happens to say, so a stale status cannot hide
 *    that.
 * 2. Otherwise the durable [DownloadAssetStatus] decides. It is written by the receiver that owns
 *    the outcome, so it knows about failures the progress store has already discarded —
 *    `mergeDownloadProgress` drops progress for FAILED and CANCELLED, which is correct for
 *    progress but would leave those rows with nothing to draw at all.
 * 3. Only with no row at all does live progress decide. That is the pre-enqueue window, and it is
 *    the only case where the progress store knows something the database does not.
 *
 * Within the row, a terminal state outranks an in-flight one, so a FAILED row that still has a
 * stale fraction on it reads as failed rather than as still going.
 */
internal fun downloadBadge(
    asset: DownloadAssetEntity?,
    progress: DownloadProgress?,
    isDownloaded: Boolean,
): DownloadBadge = when {
    isDownloaded -> DownloadBadge.DOWNLOADED

    asset != null -> when (asset.status) {
        // Treated as downloaded to match what the Downloads screen already did. The reconciliation
        // worker demotes a COMPLETED row whose file has gone to FAILED, so a lingering COMPLETED
        // here means the file is present but not yet readable, and hiding the episode offline would
        // be worse than briefly promising more than we can deliver.
        DownloadAssetStatus.COMPLETED -> DownloadBadge.DOWNLOADED

        DownloadAssetStatus.FAILED -> DownloadBadge.FAILED

        DownloadAssetStatus.CANCELLED -> DownloadBadge.CANCELLED

        DownloadAssetStatus.WAITING_FOR_WIFI -> DownloadBadge.WAITING_FOR_WIFI

        DownloadAssetStatus.RETRYING -> DownloadBadge.RETRYING

        // A queued transfer has no total size yet, so it draws an indeterminate spinner. It is
        // still under way and must not read as idle.
        DownloadAssetStatus.QUEUED,
        DownloadAssetStatus.DOWNLOADING,
        -> DownloadBadge.DOWNLOADING
    }

    progress == null -> DownloadBadge.NONE

    progress.completed -> DownloadBadge.DOWNLOADED

    progress.waitingForWifi -> DownloadBadge.WAITING_FOR_WIFI

    progress.isActive -> DownloadBadge.DOWNLOADING

    else -> DownloadBadge.NONE
}

/**
 * Whether tapping the badge should open the failure details.
 *
 * Only the two states that a user can act on. Cancelled is included because the row still holds a
 * reason and a retry is just as meaningful there.
 */
internal fun DownloadBadge.explainsFailure(): Boolean = this == DownloadBadge.FAILED || this == DownloadBadge.CANCELLED
