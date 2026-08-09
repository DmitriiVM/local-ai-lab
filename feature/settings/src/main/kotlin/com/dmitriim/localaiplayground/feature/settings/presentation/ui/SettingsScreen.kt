package com.dmitriim.localaiplayground.feature.settings.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.ui.style.AppSurfaceStyle
import com.dmitriim.localaiplayground.core.model.service.HuggingFaceCredentialStatus
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
    onRequestHuggingFaceToken: () -> Unit,
    onDismissHuggingFaceToken: () -> Unit,
    onSaveHuggingFaceToken: (String) -> Unit,
    onClearHuggingFaceToken: () -> Unit,
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
    if (state.showHuggingFaceTokenDialog) {
        HuggingFaceTokenDialog(
            saving = state.isSavingHuggingFaceToken,
            error = state.huggingFaceTokenError,
            onSave = onSaveHuggingFaceToken,
            onDismiss = onDismissHuggingFaceToken,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppSurfaceStyle.pageBackgroundBrush(MaterialTheme.colorScheme))
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                top = dimensions.topBarOverlayClearance + 20.dp,
                end = 20.dp,
                bottom = 44.dp + dimensions.bottomNavigationOverlayClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        SettingsSurfaceCard(onClick = onOpenDeviceAndRuntimes) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
        if (settings.showAdvancedControls) {
            SettingsCard("Performance defaults", styled = false) {
                EnumRadioGroup("Thread policy", settings.threadCountPolicy, ThreadCountPolicy.entries, ThreadCountPolicy::label) { value -> onUpdate { it.copy(threadCountPolicy = value) } }
                EnumRadioGroup("Unload models", settings.modelUnloadPolicy, ModelUnloadPolicy.entries, ModelUnloadPolicy::label) { value -> onUpdate { it.copy(modelUnloadPolicy = value) } }
                Toggle("Warm up selected model", settings.warmUpSelectedModel) { value -> onUpdate { it.copy(warmUpSelectedModel = value) } }
                EnumSelector("Metric detail", settings.metricDetail, MetricDetail.entries, MetricDetail::label) { value -> onUpdate { it.copy(metricDetail = value) } }
            }
        }
        SettingsCard("Appearance", styled = false) {
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
        SettingsCard("Model downloads") {
            Text("Hugging Face token", style = MaterialTheme.typography.titleSmall)
            when (state.huggingFaceCredentialStatus) {
                HuggingFaceCredentialStatus.MISSING -> {
                    Text("No token configured. Required only for gated model downloads.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onRequestHuggingFaceToken, modifier = Modifier.fillMaxWidth()) { Text("Configure token") }
                }
                HuggingFaceCredentialStatus.USER_CONFIGURED -> {
                    Text("A token is encrypted and stored on this device.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onRequestHuggingFaceToken, modifier = Modifier.fillMaxWidth()) { Text("Replace token") }
                    OutlinedButton(onClick = onClearHuggingFaceToken, modifier = Modifier.fillMaxWidth()) { Text("Remove token") }
                }
                HuggingFaceCredentialStatus.DEBUG_CONFIGURED -> {
                    Text("A development token is supplied by local.properties in this debug build.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onRequestHuggingFaceToken, modifier = Modifier.fillMaxWidth()) { Text("Use my token instead") }
                }
            }
        }
        SettingsCard("Storage & privacy") {
            StorageLine("Models", state.storage.modelsBytes)
            StorageLine("Temporary recordings", state.storage.recordingsBytes)
            StorageLine("Generated audio", state.storage.generatedAudioBytes)
            StorageLine("History", state.storage.historyBytes)
            OutlinedButton(onClick = onClearTemporaryMedia, modifier = Modifier.fillMaxWidth()) { Text("Clear temporary media") }
            OutlinedButton(
                onClick = {
                    if (settings.confirmDestructiveActions) {
                        onRequestClearRunHistory()
                    } else {
                        onClearRunHistory()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear run history") }
            Text("Inference content stays on this device. Model downloads are the only feature that uses network access, and only after you start one.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    styled: Boolean = true,
    content: @Composable () -> Unit,
) {
    SettingsSurfaceCard(styled = styled) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SettingsSurfaceCard(
    onClick: (() -> Unit)? = null,
    styled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = (if (styled) {
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = AppSurfaceStyle.cardBorderBrush(colors),
                    shape = AppSurfaceStyle.CardShape,
                )
        } else {
            Modifier.fillMaxWidth()
        })
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        shape = if (styled) AppSurfaceStyle.CardShape else MaterialTheme.shapes.medium,
        colors = if (styled) {
            CardDefaults.cardColors(containerColor = Color.Transparent)
        } else {
            CardDefaults.cardColors()
        },
        elevation = if (styled) {
            CardDefaults.cardElevation(defaultElevation = 0.dp)
        } else {
            CardDefaults.cardElevation()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (styled) {
                        Modifier.background(AppSurfaceStyle.cardBackgroundBrush(colors))
                    } else {
                        Modifier
                    },
                )
                .padding(if (styled) 18.dp else 16.dp),
        ) {
            content()
        }
    }
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
    Text(
        text = label,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyMedium,
    )
    Switch(
        checked = checked,
        onCheckedChange = onChanged,
        modifier = Modifier.scale(0.82f),
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
            checkedTrackColor = MaterialTheme.colorScheme.tertiary,
            checkedBorderColor = MaterialTheme.colorScheme.tertiary,
        ),
    )
}

@Composable
private fun <T> EnumSelector(label: String, selected: T, values: Iterable<T>, text: (T) -> String, onSelected: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value ->
                val isSelected = value == selected
                OutlinedButton(
                    onClick = { onSelected(value) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            Color.Transparent
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
                ) {
                    Text(text(value))
                }
            }
        }
    }
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
        Text(label, style = MaterialTheme.typography.titleSmall)
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
                RadioButton(
                    selected = isSelected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.tertiary,
                        unselectedColor = MaterialTheme.colorScheme.outline,
                    ),
                )
                Text(
                    text(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun StorageLine(label: String, bytes: Long) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label)
    Text(bytes.readable())
}

private fun Long.readable(): String = when {
    this < 1_024 -> "$this B"
    this < 1_048_576 -> "${this / 1_024} KB"
    else -> "${"%.1f".format(this / 1_048_576.0)} MB"
}
private val ThreadCountPolicy.label get() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private val ModelUnloadPolicy.label get() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private val MetricDetail.label get() = name.lowercase().replaceFirstChar(Char::uppercase)
