package com.shapeshed.booth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.data.EpisodeEntity
import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

@Composable
internal fun PodcastInboxSelectionHeader(
    inbox: List<EpisodeEntity>,
    selectedIds: Set<Long>,
    onSelectedIdsChange: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allSelected = inbox.isNotEmpty() && inbox.all { it.id in selectedIds }
    val selectionContentDescription = stringResource(if (allSelected) R.string.clear_selection else R.string.select_all)
    val toggleAll = { onSelectedIdsChange(if (allSelected) emptySet() else inbox.map { it.id }.toSet()) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = toggleAll)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = allSelected,
            onCheckedChange = { checked ->
                onSelectedIdsChange(if (checked) inbox.map { it.id }.toSet() else emptySet())
            },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.semantics {
                contentDescription = selectionContentDescription
            },
        )
        Text(
            text = stringResource(R.string.select_all),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
