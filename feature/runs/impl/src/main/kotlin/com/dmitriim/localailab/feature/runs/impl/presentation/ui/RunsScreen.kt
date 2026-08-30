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
import com.dmitriim.localailab.feature.runs.api.domain.history.RunKind
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
        modifier = Modifier
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
        text = {
            Text(stringResource(CoreUiR.string.runs_clear_history_explanation))
        },
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
    Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(run.id) }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

@Composable
private fun RunDetails(
    run: RunRecord,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onRepeat: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = dimensions.screenPadding),
        contentPadding = PaddingValues(
            top = dimensions.topBarOverlayClearance + 20.dp,
            bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClose) {
                    Text(stringResource(CoreUiR.string.runs_runs_screen_100))
                }
                OutlinedButton(onClick = onRepeat) {
                    Text(stringResource(CoreUiR.string.runs_runs_screen_101))
                }
                OutlinedButton(onClick = onShare) {
                    Text(stringResource(CoreUiR.string.runs_runs_screen_102))
                }
            }
        }
        item {
            Text(
                text = stringResource(
                    CoreUiR.string.runs_kind_and_capability,
                    run.kind.label(),
                    run.capability.label(),
                ),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Text(
                text = stringResource(
                    CoreUiR.string.runs_status_and_model,
                    run.status.label(),
                    run.model?.displayName ?: stringResource(CoreUiR.string.runs_model_removed),
                ),
            )
        }
        item {
            DetailCard(
                title = stringResource(CoreUiR.string.runs_input),
                body = run.input ?: stringResource(CoreUiR.string.runs_not_retained),
            )
        }
        item {
            DetailCard(
                title = stringResource(CoreUiR.string.runs_output),
                body = run.output ?: stringResource(CoreUiR.string.runs_not_available),
            )
        }
        item {
            DetailCard(
                title = stringResource(CoreUiR.string.runs_effective_parameters),
                body = run.parametersJson,
            )
        }
        item {
            DetailCard(
                title = stringResource(CoreUiR.string.runs_metrics),
                body = run.metricsJson,
            )
        }
        run.errorMessage?.let { error ->
            item {
                DetailCard(
                    title = stringResource(CoreUiR.string.runs_error),
                    body = error,
                )
            }
        }
        if (run.linkedRunIds.isNotEmpty()) {
            item {
                DetailCard(
                    title = stringResource(CoreUiR.string.runs_linked_pipeline_runs),
                    body = run.linkedRunIds.joinToString(),
                )
            }
        }
        item {
            Text(
                text = stringResource(CoreUiR.string.runs_export_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AiCapability.label(): String = stringResource(
    when (this) {
        AiCapability.CHAT -> CoreUiR.string.runs_capability_chat
        AiCapability.SPEECH_TO_TEXT -> CoreUiR.string.runs_capability_speech_to_text
        AiCapability.TEXT_TO_SPEECH -> CoreUiR.string.runs_capability_text_to_speech
        AiCapability.VOICE_ACTIVITY_DETECTION -> CoreUiR.string.runs_capability_voice_activity_detection
        AiCapability.VOICE_ASSISTANT -> CoreUiR.string.runs_capability_voice_assistant
    },
)

@Composable
private fun AiCapability.filterLabel(): String = stringResource(
    when (this) {
        AiCapability.SPEECH_TO_TEXT -> CoreUiR.string.runs_filter_stt
        AiCapability.TEXT_TO_SPEECH -> CoreUiR.string.runs_filter_tts
        AiCapability.VOICE_ASSISTANT -> CoreUiR.string.runs_filter_assistant
        else -> return label()
    },
)

@Composable
private fun RunStatus.label(): String = stringResource(
    when (this) {
        RunStatus.SUCCEEDED -> CoreUiR.string.runs_status_succeeded
        RunStatus.CANCELLED -> CoreUiR.string.runs_status_cancelled
        RunStatus.FAILED -> CoreUiR.string.runs_status_failed
    },
)

@Composable
private fun RunKind.label(): String = stringResource(
    when (this) {
        RunKind.INFERENCE -> CoreUiR.string.runs_kind_inference
        RunKind.BENCHMARK_SESSION -> CoreUiR.string.runs_kind_benchmark_session
    },
)
