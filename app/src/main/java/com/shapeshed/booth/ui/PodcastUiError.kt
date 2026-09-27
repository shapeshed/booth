package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

/**
 * A failure the screen should keep showing until something replaces it.
 *
 * Only failures. A completed import or export is a one-shot [PodcastUiEvent] instead, because it
 * stops being true once it has been shown and a value that outlives its own display gets replayed
 * on every configuration change.
 */
sealed interface PodcastUiError {
    data object SearchFailed : PodcastUiError
    data object DiscoveryLoadFailed : PodcastUiError
    data object CategoryLoadFailed : PodcastUiError
    data object SubscribeFailed : PodcastUiError
    data object PodcastLoadFailed : PodcastUiError
    data object EpisodeLoadFailed : PodcastUiError
    data object OpmlReadFailed : PodcastUiError
    data object NoPodcastFeeds : PodcastUiError
    data object ExportFailed : PodcastUiError
    data object BackupImportFailed : PodcastUiError
    data object RefreshFailed : PodcastUiError
    data object PartialRefreshFailed : PodcastUiError
    data object RemovePodcastFailed : PodcastUiError
    data object PlaybackConnectionFailed : PodcastUiError
    data object PlaybackServerFailed : PodcastUiError
    data object PlaybackFailed : PodcastUiError
}

@Composable
internal fun podcastErrorMessage(error: PodcastUiError): String = when (error) {
    PodcastUiError.SearchFailed -> stringResource(R.string.error_search_failed)
    PodcastUiError.DiscoveryLoadFailed -> stringResource(R.string.error_discovery_load_failed)
    PodcastUiError.CategoryLoadFailed -> stringResource(R.string.error_category_load_failed)
    PodcastUiError.SubscribeFailed -> stringResource(R.string.error_subscribe_failed)
    PodcastUiError.PodcastLoadFailed -> stringResource(R.string.error_podcast_load_failed)
    PodcastUiError.EpisodeLoadFailed -> stringResource(R.string.error_episode_load_failed)
    PodcastUiError.OpmlReadFailed -> stringResource(R.string.error_opml_read_failed)
    PodcastUiError.NoPodcastFeeds -> stringResource(R.string.error_no_podcast_feeds)
    PodcastUiError.ExportFailed -> stringResource(R.string.error_opml_export_failed)
    PodcastUiError.BackupImportFailed -> stringResource(R.string.error_backup_import_failed)
    PodcastUiError.RefreshFailed -> stringResource(R.string.error_refresh_failed)
    PodcastUiError.PartialRefreshFailed -> stringResource(R.string.error_partial_refresh_failed)
    PodcastUiError.RemovePodcastFailed -> stringResource(R.string.error_remove_podcast_failed)
    PodcastUiError.PlaybackConnectionFailed -> stringResource(R.string.error_playback_connection)
    PodcastUiError.PlaybackServerFailed -> stringResource(R.string.error_playback_server)
    PodcastUiError.PlaybackFailed -> stringResource(R.string.error_playback_failed)
}
