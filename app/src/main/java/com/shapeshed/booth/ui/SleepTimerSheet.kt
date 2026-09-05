package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.R
import com.shapeshed.booth.BuildConfig
import com.shapeshed.booth.data.SleepTimerState

private const val MINUTE_MS = 60_000L
private val SleepTimerPresets = buildList {
    if (BuildConfig.DEBUG) add(30_000L)
    addAll(listOf(15, 30, 45, 60, 90).map { it * MINUTE_MS })
}

internal fun formatSleepRemaining(ms: Long): String {
    val totalSeconds = ((ms + 999L) / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SleepTimerAction(
    active: SleepTimerState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (active == null) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Icon(
                Icons.Rounded.Bedtime,
                contentDescription = stringResource(R.string.sleep_timer),
                modifier = Modifier.scale(0.85f),
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                formatSleepRemaining(active.remainingMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalIconButton(
                onClick = onClick,
            ) {
                Icon(
                    Icons.Rounded.Bedtime,
                    contentDescription = stringResource(R.string.sleep_timer),
                    modifier = Modifier.scale(0.85f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun SleepTimerSheet(
    active: SleepTimerState?,
    onSet: (Long) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var selectedMinutes by remember(active?.totalMs) {
            mutableFloatStateOf(
                ((active?.totalMs ?: 30 * MINUTE_MS).toFloat() / MINUTE_MS)
                    .coerceIn(1f, 120f),
            )
        }
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.sleep_timer), style = MaterialTheme.typography.titleLarge)
            if (active != null) {
                Text(formatSleepRemaining(active.remainingMs), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                val progress = (1f - active.remainingMs.toFloat() / active.totalMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                LinearWavyProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth().height(WavyProgressIndicatorDefaults.LinearContainerHeight),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { onCancel(); onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.cancel))
                    }
                    FilledTonalButton(onClick = { onSet(active.remainingMs + 15 * MINUTE_MS) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.sleep_add_15))
                    }
                }
                Text(stringResource(R.string.sleep_set_new_duration), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SleepTimerPresets.forEach { durationMs ->
                    val label = if (durationMs < MINUTE_MS) {
                        pluralStringResource(
                            R.plurals.sleep_preset_seconds,
                            (durationMs / 1_000L).toInt(),
                            (durationMs / 1_000L).toInt(),
                        )
                    } else {
                        pluralStringResource(
                            R.plurals.sleep_preset_minutes,
                            (durationMs / MINUTE_MS).toInt(),
                            (durationMs / MINUTE_MS).toInt(),
                        )
                    }
                    PlaybackPresetButton(
                        label = label,
                        selected = active?.totalMs == durationMs,
                        onClick = { onSet(durationMs); onDismiss() },
                    )
                }
            }
            Text(
                formatSleepRemaining(selectedMinutes.roundToInt() * MINUTE_MS),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = selectedMinutes,
                onValueChange = { selectedMinutes = it },
                onValueChangeFinished = { onSet(selectedMinutes.roundToInt() * MINUTE_MS) },
                valueRange = 1f..120f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
