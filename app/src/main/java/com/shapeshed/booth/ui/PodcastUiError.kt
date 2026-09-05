package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.shapeshed.booth.R

sealed interface PodcastUiError {
    data object SearchFailed : PodcastUiError
    data object DiscoveryLoadFailed : PodcastUiError
    data object CategoryLoadFailed : PodcastUiError
    data object SubscribeFailed : PodcastUiError
    data object PodcastLoadFailed : PodcastUiError
    data object EpisodeLoadFailed : PodcastUiError
    data object OpmlReadFailed : PodcastUiError
    data object NoPodcastFeeds : PodcastUiError
    data class ImportCompleted(val imported: Int, val total: Int) : PodcastUiError
    data object ExportFailed : PodcastUiError
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
    is PodcastUiError.ImportCompleted -> if (error.imported == error.total) {
        pluralStringResource(R.plurals.imported_podcasts, error.imported, error.imported)
    } else {
        pluralStringResource(
            R.plurals.imported_podcasts_partial,
            error.imported,
            error.imported,
            error.total,
        )
    }
    PodcastUiError.ExportFailed -> stringResource(R.string.error_opml_export_failed)
    PodcastUiError.RefreshFailed -> stringResource(R.string.error_refresh_failed)
    PodcastUiError.PartialRefreshFailed -> stringResource(R.string.error_partial_refresh_failed)
    PodcastUiError.RemovePodcastFailed -> stringResource(R.string.error_remove_podcast_failed)
    PodcastUiError.PlaybackConnectionFailed -> stringResource(R.string.error_playback_connection)
    PodcastUiError.PlaybackServerFailed -> stringResource(R.string.error_playback_server)
    PodcastUiError.PlaybackFailed -> stringResource(R.string.error_playback_failed)
}
