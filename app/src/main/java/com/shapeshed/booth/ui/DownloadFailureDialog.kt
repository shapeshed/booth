package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.R
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.MAX_DOWNLOAD_RETRIES
import com.shapeshed.booth.data.isDownloadPermanentlyUnavailable
import kotlinx.coroutines.launch

/**
 * Which episode's download failure is currently being explained, if any.
 *
 * A holder rather than a raw `mutableStateOf` in each screen so the open/close logic and the
 * non-null asset lookup live in one place. The three list views each used to keep their own copy of
 * this, which is how they drifted apart in the first place.
 */
@Stable
internal class DownloadFailureState {
    var episode: EpisodeEntity? by mutableStateOf(null)
        private set

    fun show(episode: EpisodeEntity) {
        this.episode = episode
    }

    fun dismiss() {
        episode = null
    }
}

@Composable
internal fun rememberDownloadFailureState(): DownloadFailureState = remember { DownloadFailureState() }

/**
 * Shows the failure dialog for whichever episode [state] is holding, and wires the actions.
 *
 * The coroutine scope is deliberately created here, outside the null check, rather than beside the
 * dialog. It used to be inside the `episode?.let` block, which meant clearing the state removed the
 * block and cancelled the remembered scope while `onRemoveDownload` was still suspended waiting on the
 * undo snackbar, so a removal started from the dialog could be cancelled part-way.
 */
@Composable
internal fun DownloadFailureDialogHost(
    state: DownloadFailureState,
    assetsByEpisodeId: Map<Long, DownloadAssetEntity>,
    onRetry: (episodeId: Long) -> Unit,
    onRemove: suspend (episodeId: Long) -> Result<Unit>,
) {
    val scope = rememberCoroutineScope()
    state.episode?.let { failing ->
        DownloadFailureDialog(
            episodeTitle = failing.title,
            asset = assetsByEpisodeId[failing.id],
            onRetry = {
                state.dismiss()
                onRetry(failing.id)
            },
            onRemove = {
                state.dismiss()
                scope.launch { onRemove(failing.id) }
            },
            onDismiss = { state.dismiss() },
        )
    }
}

/**
 * Explains a failed download and offers a retry.
 *
 * The badge itself is deliberately unlabelled apart from "Download failed": a 16dp icon cannot
 * carry a reason. This is where the detail goes, and it is reachable from any list view, because
 * the badge that opens it is drawn by the shared [EpisodeDownloadBadge].
 *
 * The reason shown is the one the download receiver recorded, which is Booth's own wording for the
 * platform error rather than a raw code.
 *
 * [onRetry] resets the stored retry count as well as re-enqueueing. That matters: a row that has
 * exhausted its retries would otherwise be refused by `shouldRetryDownload` on its very next
 * failure, so a retry that only re-enqueued would silently do nothing for exactly the downloads
 * the user most wants to retry.
 */
@Composable
internal fun DownloadFailureDialog(
    episodeTitle: String,
    asset: DownloadAssetEntity?,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelled = asset?.status == DownloadAssetStatus.CANCELLED
    // The recorded reason already carries the right wording for a gone enclosure: the mapping in
    // DownloadManagerErrors turns an HTTP 404 into DOWNLOAD_UNAVAILABLE_MESSAGE. Matching on rendered
    // text here to decide the same thing again was both brittle and redundant.
    val recordedReason = asset?.errorMessage?.takeIf { it.isNotBlank() }
    val reasonDescription = recordedReason ?: stringResource(R.string.download_failed_unknown_reason)
    val goneForGood = isDownloadPermanentlyUnavailable(recordedReason)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (cancelled) R.string.download_cancelled_title else R.string.download_failed_title,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = episodeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val sizeBytes = asset?.totalBytes
                if (sizeBytes != null && sizeBytes > 0L) {
                    Text(
                        text = stringResource(R.string.download_failure_size, formatFileSize(sizeBytes)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // No contentDescription here: the text is already readable, and adding one that
                // repeats it makes TalkBack announce the reason twice.
                Text(
                    text = reasonDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (goneForGood) {
                        stringResource(R.string.download_failure_gone_forever)
                    } else {
                        stringResource(R.string.download_failure_retry_policy, MAX_DOWNLOAD_RETRIES)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            // Two actions on one line, each with maxLines = 1: three buttons across a phone width
            // wrapped "Retry" mid-word, which reads as a layout bug rather than a choice.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onRemove) {
                    Text(
                        text = stringResource(R.string.remove_download),
                        maxLines = 1,
                    )
                }
                TextButton(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.retry_download),
                        maxLines = 1,
                    )
                }
            }
        },
        // Cancel belongs in dismissButton, not confirmButton: it is the dismissive action, and
        // Material 3 places it on the leading side for that reason.
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), maxLines = 1)
            }
        },
    )
}
