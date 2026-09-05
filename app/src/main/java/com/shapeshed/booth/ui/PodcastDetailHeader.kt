package com.shapeshed.booth.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun PodcastDetailHeader(
    title: String,
    artworkUrl: String?,
    onArtworkClick: (() -> Unit)? = null,
    author: String?,
    description: String? = null,
    onDescriptionClick: (() -> Unit)? = null,
    categories: List<String> = emptyList(),
    onCategory: (String) -> Unit = {},
    isSubscribed: Boolean,
    onSubscription: () -> Unit,
    showSubscriptionAction: Boolean = true,
    latestAction: @Composable (Modifier) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PodcastArtwork(
                imageUrl = artworkUrl,
                title = title,
                modifier = Modifier.size(144.dp),
                onClick = onArtworkClick,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                author?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                description?.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        modifier = Modifier.clickable(
                            enabled = onDescriptionClick != null,
                            onClick = { onDescriptionClick?.invoke() },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (categories.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.take(5).forEach { category ->
                        AssistChip(onClick = { onCategory(category) }, label = { Text(category) })
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            latestAction(Modifier.weight(1f))
            if (showSubscriptionAction) {
                FilledTonalIconButton(onClick = onSubscription) {
                    Icon(
                        imageVector = if (isSubscribed) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = stringResource(if (isSubscribed) R.string.unfollow else R.string.follow),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PodcastLatestEpisodeButton(
    durationMs: Long?,
    positionMs: Long,
    completed: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EpisodePlaybackButton(
        modifier = modifier,
        durationMs = durationMs,
        positionMs = positionMs,
        completed = completed,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        labelOverride = if (!completed && positionMs <= 0L && !isPlaying && !isBuffering) {
            stringResource(R.string.play_latest_episode)
        } else {
            null
        },
        onClick = onClick,
        prominent = true,
    )
}
