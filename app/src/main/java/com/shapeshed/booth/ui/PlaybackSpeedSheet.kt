package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.R
import java.util.Locale

internal const val MIN_PLAYBACK_SPEED = 0.5f
internal const val MAX_PLAYBACK_SPEED = 3f

private val PlaybackSpeedPresets = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
internal fun PlaybackPresetButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = if (selected) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
    ) {
        Text(label)
    }
}

private fun standardSpeedIcon(speed: Float): androidx.compose.ui.graphics.vector.ImageVector? = when (speed) {
    0.75f -> speed_0_75
    1.25f -> speed_1_25
    1.5f -> speed_1_5
    2f -> speed_2x
    else -> null
}

internal fun isStandardPlaybackSpeed(speed: Float): Boolean =
    PlaybackSpeedPresets.any { it == speed }

internal fun formatPlaybackSpeed(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}×" else "${String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')}×"

@Composable
internal fun PlaybackSpeedAction(
    speed: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(onClick = onClick, modifier = modifier) {
        standardSpeedIcon(speed)?.let { icon ->
            Icon(icon, contentDescription = stringResource(R.string.playback_speed_custom, formatPlaybackSpeed(speed)))
        } ?: Icon(
            Icons.Outlined.Speed,
            contentDescription = stringResource(R.string.playback_speed_custom, formatPlaybackSpeed(speed)),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackSpeedSheet(
    speed: Float,
    skipSilence: Boolean,
    onSpeedChange: (Float) -> Unit,
    onSkipSilenceChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderSpeed by remember(speed) { mutableFloatStateOf(speed) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.playback_speed), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = formatPlaybackSpeed(sliderSpeed),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaybackSpeedPresets.forEach { preset ->
                    PlaybackPresetButton(
                        label = formatPlaybackSpeed(preset),
                        selected = speed == preset,
                        onClick = {
                            sliderSpeed = preset
                            onSpeedChange(preset)
                        },
                    )
                }
            }
            Slider(
                value = sliderSpeed,
                onValueChange = {
                    sliderSpeed = it
                    onSpeedChange(it)
                },
                valueRange = MIN_PLAYBACK_SPEED..MAX_PLAYBACK_SPEED,
                steps = 24,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.skip_silence), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.skip_silence_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Checkbox(
                    checked = skipSilence,
                    onCheckedChange = onSkipSilenceChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun PodcastPlaybackSpeedSheet(
    speed: Float,
    inherited: Boolean,
    globalSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onUseGlobal: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderSpeed by remember(speed) { mutableFloatStateOf(speed) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.playback_speed), style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (inherited) {
                    stringResource(R.string.inherited_global_playback_speed, formatPlaybackSpeed(globalSpeed))
                } else {
                    formatPlaybackSpeed(sliderSpeed)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlaybackPresetButton(
                label = stringResource(R.string.use_global_playback_speed),
                selected = inherited,
                onClick = onUseGlobal,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaybackSpeedPresets.forEach { preset ->
                    PlaybackPresetButton(
                        label = formatPlaybackSpeed(preset),
                        selected = !inherited && sliderSpeed == preset,
                        onClick = {
                            sliderSpeed = preset
                            onSpeedChange(preset)
                        },
                    )
                }
            }
            Slider(
                value = sliderSpeed,
                onValueChange = {
                    sliderSpeed = it
                    onSpeedChange(it)
                },
                valueRange = MIN_PLAYBACK_SPEED..MAX_PLAYBACK_SPEED,
                steps = 24,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
