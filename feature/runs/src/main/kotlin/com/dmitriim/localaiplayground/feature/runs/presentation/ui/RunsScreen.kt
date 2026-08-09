package com.dmitriim.localaiplayground.feature.runs.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.runs.RunKind
import com.dmitriim.localaiplayground.core.model.runs.RunStatus
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.core.ui.style.AppSurfaceStyle
import com.dmitriim.localaiplayground.feature.runs.presentation.RunsUiState
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
        RunDetails(selected, onCloseDetails, onShare, onRepeat)
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
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
                Text("Runs", style = MaterialTheme.typography.headlineMedium)
                RunsOverflowMenu(
                    hasRuns = state.runs.isNotEmpty(),
                    onClearRunHistory = onRequestClearRunHistory,
                )
            }
        }
        item {
            Text(
                text = "Saved settings, results, and metrics remain readable " +
                    "even when a model is later removed.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { FilterRow(state, onCapabilityFilter, onStatusFilter) }
        if (state.filteredRuns.isEmpty()) {
            item {
                StatusMessage(
                    title = "No matching runs",
                    explanation = "Completed, cancelled, and failed local inference " +
                        "attempts will appear here.",
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
        title = { Text("Clear run history?") },
        text = {
            Text(
                "This removes saved runs, but leaves conversations, installed models, " +
                    "and exported files untouched.",
            )
        },
        confirmButton = {
            OutlinedButton(onClick = onConfirm) { Text("Clear run history") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
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
                contentDescription = "More run actions",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Clear run history") },
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
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = AppSurfaceStyle.cardBorderBrush(colors),
                shape = AppSurfaceStyle.CardShape,
            ),
        shape = AppSurfaceStyle.CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppSurfaceStyle.cardBackgroundBrush(colors))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.capability == null,
                    onClick = { onCapability(null) },
                    label = { Text("All") },
                    colors = runFilterChipColors(),
                )
            }
            items(runFilterCapabilities.size) { index ->
                val value = runFilterCapabilities[index]
                FilterChip(
                    selected = state.capability == value,
                    onClick = { onCapability(value) },
                    label = { Text(value.filterLabel) },
                    colors = runFilterChipColors(),
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.status == null,
                    onClick = { onStatus(null) },
                    label = { Text("All") },
                    colors = runFilterChipColors(),
                )
            }
            items(RunStatus.entries.size) { index ->
                val value = RunStatus.entries[index]
                FilterChip(
                    selected = state.status == value,
                    onClick = { onStatus(value) },
                    label = { Text(value.label) },
                    colors = runFilterChipColors(),
                )
            }
        }
        }
    }
}

@Composable
private fun runFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
    containerColor = Color.Transparent,
)

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
                text = "${run.kind.label} · ${run.capability.label} · ${run.status.label}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = run.model?.displayName ?: "Model snapshot unavailable",
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = dimensions.topBarOverlayClearance + 20.dp,
            bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClose) { Text("Back") }
                OutlinedButton(onClick = onRepeat) { Text("Repeat") }
                OutlinedButton(onClick = onShare) { Text("Export JSON") }
            }
        }
        item {
            Text(
                text = "${run.kind.label} · ${run.capability.label}",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Text(
                text = "${run.status.label} · " +
                    (run.model?.displayName ?: "removed or unavailable model"),
            )
        }
        item { DetailCard("Input", run.input ?: "Not retained") }
        item { DetailCard("Output", run.output ?: "Not available") }
        item { DetailCard("Effective parameters", run.parametersJson) }
        item { DetailCard("Metrics", run.metricsJson) }
        run.errorMessage?.let { error ->
            item { DetailCard("Error", error) }
        }
        if (run.linkedRunIds.isNotEmpty()) {
            item {
                DetailCard("Linked pipeline runs", run.linkedRunIds.joinToString())
            }
        }
        item {
            Text(
                text = "Export contains this text and metadata. Audio, private " +
                    "file paths, and source URIs are never included.",
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

private val AiCapability.label get() = when (this) {
    AiCapability.CHAT -> "Chat"
    AiCapability.SPEECH_TO_TEXT -> "Speech to text"
    AiCapability.TEXT_TO_SPEECH -> "Text to speech"
    AiCapability.VOICE_ACTIVITY_DETECTION -> "Voice activity detection"
    AiCapability.VOICE_ASSISTANT -> "Voice assistant"
}

private val AiCapability.filterLabel get() = when (this) {
    AiCapability.SPEECH_TO_TEXT -> "STT"
    AiCapability.TEXT_TO_SPEECH -> "TTS"
    AiCapability.VOICE_ASSISTANT -> "Assistant"
    else -> label
}

private val RunStatus.label get() = name.lowercase().replaceFirstChar(Char::uppercase)

private val RunKind.label get() = when (this) {
    RunKind.INFERENCE -> "Inference"
    RunKind.BENCHMARK_SESSION -> "Benchmark session"
}
