package com.dmitriim.localaiplayground.feature.chat.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatSettings

@Composable
internal fun ChatSettingsDialog(
    settings: ChatSettings,
    onSettingsChange: ((ChatSettings) -> ChatSettings) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generation settings") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ChatSettingField("System prompt", settings.systemPrompt) { value -> onSettingsChange { it.copy(systemPrompt = value) } } }
                item { ChatSettingField("Temperature (0–2)", settings.temperature) { value -> onSettingsChange { it.copy(temperature = value) } } }
                item { ChatSettingField("Top-K (1–200)", settings.topK) { value -> onSettingsChange { it.copy(topK = value) } } }
                item { ChatSettingField("Top-P (0.05–1)", settings.topP) { value -> onSettingsChange { it.copy(topP = value) } } }
                item { ChatSettingField("Maximum output tokens", settings.maxOutputTokens) { value -> onSettingsChange { it.copy(maxOutputTokens = value) } } }
                item { ChatSettingField("Seed (-1 = random)", settings.seed) { value -> onSettingsChange { it.copy(seed = value) } } }
                item { ChatSettingField("Context size", settings.contextSize) { value -> onSettingsChange { it.copy(contextSize = value) } } }
                item { ChatSettingField("Thread count (0 = safe default)", settings.threadCount) { value -> onSettingsChange { it.copy(threadCount = value) } } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onReset) { Text("Reset") } },
    )
}

@Composable
private fun ChatSettingField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}
