package com.dmitriim.localaiplayground.feature.settings.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.feature.settings.presentation.SettingsUiState
import com.dmitriim.localaiplayground.source.settings.AppSettings
import com.dmitriim.localaiplayground.source.settings.MetricDetail
import com.dmitriim.localaiplayground.source.settings.ModelUnloadPolicy
import com.dmitriim.localaiplayground.source.settings.ThemePreference
import com.dmitriim.localaiplayground.source.settings.ThreadCountPolicy

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenDeviceAndRuntimes: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onClearTemporaryMedia: () -> Unit,
    onRequestClearRunHistory: () -> Unit,
    onDismissClearRunHistory: () -> Unit,
    onClearRunHistory: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val settings = state.settings
    if (state.pendingRunHistoryClear) {
        AlertDialog(
            onDismissRequest = onDismissClearRunHistory,
            title = { Text("Clear run history?") },
            text = { Text("This removes saved runs, but leaves conversations, installed models, and exported files untouched.") },
            confirmButton = { OutlinedButton(onClick = onClearRunHistory) { Text("Clear run history") } },
            dismissButton = { OutlinedButton(onClick = onDismissClearRunHistory) { Text("Cancel") } },
        )
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, top = dimensions.topBarOverlayClearance + 20.dp, end = 20.dp, bottom = 44.dp + dimensions.bottomNavigationOverlayClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Card(
            onClick = onOpenDeviceAndRuntimes,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Device & runtimes", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Hardware, runtime availability, and diagnostics",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineSmall)
            }
        }
        SettingsCard("Appearance") {
            EnumRadioGroup("Theme", settings.theme, ThemePreference.entries, ThemePreference::name) { value -> onUpdate { it.copy(theme = value) } }
            Toggle("Keep screen awake during active inference", settings.keepScreenAwake) { value -> onUpdate { it.copy(keepScreenAwake = value) } }
            Toggle("Confirm before deleting history", settings.confirmDestructiveActions) { value -> onUpdate { it.copy(confirmDestructiveActions = value) } }
            Toggle("Show advanced controls", settings.showAdvancedControls) { value -> onUpdate { it.copy(showAdvancedControls = value) } }
        }
        SettingsCard("Retention") {
            Text("Microphone recordings: session only", style = MaterialTheme.typography.titleSmall)
            Text("Recordings are temporary and are cleared when you clear media.", style = MaterialTheme.typography.bodySmall)
            Text("Generated audio: latest successful WAV", style = MaterialTheme.typography.titleSmall)
            Text("The app retains one successful WAV until the next successful synthesis, as documented in the Stage 5 policy.", style = MaterialTheme.typography.bodySmall)
        }
        if (settings.showAdvancedControls) SettingsCard("Performance defaults") {
            EnumRadioGroup("Thread policy", settings.threadCountPolicy, ThreadCountPolicy.entries, ThreadCountPolicy::label) { value -> onUpdate { it.copy(threadCountPolicy = value) } }
            EnumRadioGroup("Unload models", settings.modelUnloadPolicy, ModelUnloadPolicy.entries, ModelUnloadPolicy::label) { value -> onUpdate { it.copy(modelUnloadPolicy = value) } }
            Toggle("Warm up selected model", settings.warmUpSelectedModel) { value -> onUpdate { it.copy(warmUpSelectedModel = value) } }
            EnumSelector("Metric detail", settings.metricDetail, MetricDetail.entries, MetricDetail::label) { value -> onUpdate { it.copy(metricDetail = value) } }
        }
        SettingsCard("Storage & privacy") {
            StorageLine("Models", state.storage.modelsBytes)
            StorageLine("Temporary recordings", state.storage.recordingsBytes)
            StorageLine("Generated audio", state.storage.generatedAudioBytes)
            StorageLine("History", state.storage.historyBytes)
            OutlinedButton(onClick = onClearTemporaryMedia, modifier = Modifier.fillMaxWidth()) { Text("Clear temporary media") }
            OutlinedButton(
                onClick = {
                    if (settings.confirmDestructiveActions) onRequestClearRunHistory()
                    else onClearRunHistory()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear run history") }
            Text("Inference content stays on this device. Model downloads are the only feature that uses network access, and only after you start one.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); content() }
}

@Composable
private fun Toggle(
    label: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(label, Modifier.weight(1f))
    Switch(
        checked = checked,
        onCheckedChange = onChanged,
        modifier = Modifier.scale(0.82f),
    )
}

@Composable
private fun <T> EnumSelector(label: String, selected: T, values: Iterable<T>, text: (T) -> String, onSelected: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(label); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { values.forEach { value -> OutlinedButton(onClick = { onSelected(value) }) { Text(text(value) + if (value == selected) " ✓" else "") } } } }
}

@Composable
private fun <T> EnumRadioGroup(
    label: String,
    selected: T,
    values: Iterable<T>,
    text: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label)
        values.forEach { value ->
            val isSelected = value == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.RadioButton) { onSelected(value) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Text(text(value))
            }
        }
    }
}

@Composable
private fun StorageLine(label: String, bytes: Long) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(bytes.readable()) }

private fun Long.readable(): String = when { this < 1_024 -> "$this B"; this < 1_048_576 -> "${this / 1_024} KB"; else -> "${"%.1f".format(this / 1_048_576.0)} MB" }
private val ThreadCountPolicy.label get() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private val ModelUnloadPolicy.label get() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private val MetricDetail.label get() = name.lowercase().replaceFirstChar(Char::uppercase)
