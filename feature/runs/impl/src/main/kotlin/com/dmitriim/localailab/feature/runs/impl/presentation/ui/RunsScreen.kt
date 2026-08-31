package com.dmitriim.localailab.feature.runs.impl.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.AppSurfaceCard
import com.dmitriim.localailab.core.ui.component.StatusMessage
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.core.ui.style.AppFilterChipDefaults
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import com.dmitriim.localailab.feature.runs.impl.presentation.RunsUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun RunsScreen(
    state: RunsUiState,
    onCapabilityFilter: (AiCapability?) -> Unit,
    onStatusFilter: (RunStatus?) -> Unit,
    onSelectRun: (String) -> Unit,
    onCloseDetails: () -> Unit,
    onRequestClearRunHistory: () -> Unit,
    onDismissClearRunHistory: () -> Unit,
    onClearRunHistory: () -> Unit,
    onShare: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current
    val selected = state.selectedRun
    if (state.pendingRunHistoryClear) {
        ClearRunHistoryDialog(
            onDismiss = onDismissClearRunHistory,
            onConfirm = onClearRunHistory,
        )
    }
    if (selected != null) {
        RunDetails(
            run = selected,
            onClose = onCloseDetails,
            onShare = onShare,
            onRepeat = onRepeat,
        )
        return
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.screenPadding),
        contentPadding = PaddingValues(
            top = dimensions.topBarOverlayClearance + 20.dp,
            bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(CoreUiR.string.runs_runs_screen_92),
                    style = MaterialTheme.typography.headlineMedium,
                )
                RunsOverflowMenu(
                    hasRuns = state.runs.isNotEmpty(),
                    onClearRunHistory = onRequestClearRunHistory,
                )
            }
        }
        item {
            Text(
                text = stringResource(CoreUiR.string.runs_history_retention_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { FilterRow(state, onCapabilityFilter, onStatusFilter) }
        if (state.filteredRuns.isEmpty()) {
            item {
                StatusMessage(
                    title = stringResource(CoreUiR.string.ui_copy_59),
                    explanation = stringResource(CoreUiR.string.runs_no_matching_explanation),
                )
            }
        } else {
            items(state.filteredRuns.size, key = { state.filteredRuns[it].id }) { index ->
                RunRow(state.filteredRuns[index], onSelectRun)
            }
        }
    }
}

@Composable
private fun ClearRunHistoryDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(CoreUiR.string.runs_runs_screen_93)) },
        text = { Text(stringResource(CoreUiR.string.runs_clear_history_explanation)) },
        confirmButton = {
            OutlinedButton(onClick = onConfirm) {
                Text(stringResource(CoreUiR.string.runs_runs_screen_95))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(CoreUiR.string.runs_runs_screen_96))
            }
        },
    )
}

@Composable
private fun RunsOverflowMenu(
    hasRuns: Boolean,
    onClearRunHistory: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(CoreUiR.string.runs_more_actions),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(CoreUiR.string.runs_runs_screen_97)) },
                onClick = {
                    expanded = false
                    onClearRunHistory()
                },
                enabled = hasRuns,
            )
        }
    }
}

@Composable
private fun FilterRow(
    state: RunsUiState,
    onCapability: (AiCapability?) -> Unit,
    onStatus: (RunStatus?) -> Unit,
) {
    AppSurfaceCard(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.capability == null,
                    onClick = { onCapability(null) },
                    label = { Text(stringResource(CoreUiR.string.runs_runs_screen_98)) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
            items(runFilterCapabilities.size) { index ->
                val value = runFilterCapabilities[index]
                FilterChip(
                    selected = state.capability == value,
                    onClick = { onCapability(value) },
                    label = { Text(value.filterLabel()) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.status == null,
                    onClick = { onStatus(null) },
                    label = { Text(stringResource(CoreUiR.string.runs_runs_screen_99)) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
            items(RunStatus.entries.size) { index ->
                val value = RunStatus.entries[index]
                FilterChip(
                    selected = state.status == value,
                    onClick = { onStatus(value) },
                    label = { Text(value.label()) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
    }
}

private val runFilterCapabilities = listOf(
    AiCapability.SPEECH_TO_TEXT,
    AiCapability.TEXT_TO_SPEECH,
    AiCapability.VOICE_ASSISTANT,
)

@Composable
private fun RunRow(run: RunRecord, onSelect: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(run.id) },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    CoreUiR.string.runs_row_summary,
                    run.kind.label(),
                    run.capability.label(),
                    run.status.label(),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = run.model?.displayName ?: stringResource(CoreUiR.string.runs_model_snapshot_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = run.input?.take(110)?.replace('\n', ' ') ?: run.errorMessage.orEmpty(),
                maxLines = 2,
            )
            Text(
                text = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT,
                    DateFormat.SHORT,
                ).format(Date(run.completedAtEpochMs)),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
