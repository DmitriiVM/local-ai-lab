package com.dmitriim.localaiplayground.feature.runs.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.runs.RunStatus
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
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
    onShare: () -> Unit,
    onRepeat: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val selected = state.selectedRun
    if (selected != null) {
        RunDetails(selected, onCloseDetails, onShare, onRepeat)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = dimensions.topBarOverlayClearance + 20.dp, bottom = 24.dp + dimensions.bottomNavigationOverlayClearance),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Runs", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Saved settings, results, and metrics remain readable even when a model is later removed.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { FilterRow(state, onCapabilityFilter, onStatusFilter) }
        if (state.filteredRuns.isEmpty()) {
            item { StatusMessage("No matching runs", "Completed, cancelled, and failed local inference attempts will appear here.") }
        } else {
            items(state.filteredRuns.size, key = { state.filteredRuns[it].id }) { index ->
                RunRow(state.filteredRuns[index], onSelectRun)
            }
        }
    }
}

@Composable
private fun FilterRow(state: RunsUiState, onCapability: (AiCapability?) -> Unit, onStatus: (RunStatus?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { onCapability(null) }, label = { Text(if (state.capability == null) "All capabilities ✓" else "All capabilities") })
            AiCapability.entries.forEach { value -> AssistChip(onClick = { onCapability(value) }, label = { Text(value.label + if (state.capability == value) " ✓" else "") }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = { onStatus(null) }, label = { Text(if (state.status == null) "All results ✓" else "All results") })
            RunStatus.entries.forEach { value -> AssistChip(onClick = { onStatus(value) }, label = { Text(value.label + if (state.status == value) " ✓" else "") }) }
        }
    }
}

@Composable
private fun RunRow(run: RunRecord, onSelect: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onSelect(run.id) }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${run.capability.label} · ${run.status.label}", style = MaterialTheme.typography.titleMedium)
            Text(run.model?.displayName ?: "Model snapshot unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(run.input?.take(110)?.replace('\n', ' ') ?: run.errorMessage.orEmpty(), maxLines = 2)
            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(run.completedAtEpochMs)), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RunDetails(run: RunRecord, onClose: () -> Unit, onShare: () -> Unit, onRepeat: () -> Unit) {
    val dimensions = LocalAppDimensions.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = dimensions.topBarOverlayClearance + 20.dp, bottom = 24.dp + dimensions.bottomNavigationOverlayClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClose) { Text("Back") }
                OutlinedButton(onClick = onRepeat) { Text("Repeat") }
                OutlinedButton(onClick = onShare) { Text("Export JSON") }
            }
        }
        item { Text("${run.capability.label} run", style = MaterialTheme.typography.headlineMedium) }
        item { Text("${run.status.label} · ${run.model?.displayName ?: "removed or unavailable model"}") }
        item { DetailCard("Input", run.input ?: "Not retained") }
        item { DetailCard("Output", run.output ?: "Not available") }
        item { DetailCard("Effective parameters", run.parametersJson) }
        item { DetailCard("Metrics", run.metricsJson) }
        run.errorMessage?.let { error -> item { DetailCard("Error", error) } }
        if (run.linkedRunIds.isNotEmpty()) item { DetailCard("Linked pipeline runs", run.linkedRunIds.joinToString()) }
        item { Text("Export contains this text and metadata. Audio, private file paths, and source URIs are never included.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun DetailCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    } }
}

private val AiCapability.label get() = when (this) {
    AiCapability.CHAT -> "Chat"
    AiCapability.SPEECH_TO_TEXT -> "Speech to text"
    AiCapability.TEXT_TO_SPEECH -> "Text to speech"
    AiCapability.VOICE_ACTIVITY_DETECTION -> "Voice activity detection"
    AiCapability.VOICE_ASSISTANT -> "Voice assistant"
}

private val RunStatus.label get() = name.lowercase().replaceFirstChar(Char::uppercase)
