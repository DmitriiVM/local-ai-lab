package com.dmitriim.localaiplayground.feature.voice.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceLanguage
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceModelOption
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceSettings
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceUiState

@Composable
internal fun VoicePipelineConfiguration(
    state: VoiceUiState,
    enabled: Boolean,
    onSelectSpeechModel: (ModelId) -> Unit,
    onSelectChatModel: (ModelId) -> Unit,
    onSelectVoiceModel: (ModelId) -> Unit,
    onSelectLanguage: (VoiceLanguage) -> Unit,
    onUpdateSettings: ((VoiceSettings) -> VoiceSettings) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pipeline configuration", style = MaterialTheme.typography.titleMedium)
            VoiceModelPicker("Speech to text", state.speechModels, state.selectedSpeechModelId, enabled, onSelectSpeechModel)
            VoiceModelPicker("Chat model", state.chatModels, state.selectedChatModelId, enabled, onSelectChatModel)
            VoiceModelPicker("Text to speech", state.voiceModels, state.selectedVoiceModelId, enabled, onSelectVoiceModel)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = state.language == language,
                        onClick = { onSelectLanguage(language) },
                        enabled = enabled,
                        label = { Text(language.label) },
                    )
                }
            }
            state.estimatedPipelineRamBytes?.let { bytes ->
                Text(
                    "Combined model estimate: ${bytes.toReadableBytes()}. Engines are loaded one at a time for this MVP.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                value = state.settings.systemPrompt,
                onValueChange = { value -> onUpdateSettings { it.copy(systemPrompt = value) } },
                enabled = enabled,
                label = { Text("System prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            VoiceSettingRow(
                firstValue = state.settings.temperature,
                firstLabel = "Temperature",
                onFirstChange = { value -> onUpdateSettings { it.copy(temperature = value) } },
                secondValue = state.settings.maxOutputTokens,
                secondLabel = "Max output tokens",
                onSecondChange = { value -> onUpdateSettings { it.copy(maxOutputTokens = value.filter(Char::isDigit)) } },
                enabled = enabled,
            )
            VoiceSettingRow(
                firstValue = state.settings.contextSize,
                firstLabel = "Context tokens",
                onFirstChange = { value -> onUpdateSettings { it.copy(contextSize = value.filter(Char::isDigit)) } },
                secondValue = state.settings.speechRate,
                secondLabel = "Speech rate",
                onSecondChange = { value -> onUpdateSettings { it.copy(speechRate = value) } },
                enabled = enabled,
            )
            VoiceSettingRow(
                firstValue = state.settings.sttThreadCount,
                firstLabel = "STT threads",
                onFirstChange = { value -> onUpdateSettings { it.copy(sttThreadCount = value.filter(Char::isDigit)) } },
                secondValue = state.settings.llmThreadCount,
                secondLabel = "LLM threads",
                onSecondChange = { value -> onUpdateSettings { it.copy(llmThreadCount = value.filter(Char::isDigit)) } },
                enabled = enabled,
            )
            VoiceSettingRow(
                firstValue = state.settings.ttsThreadCount,
                firstLabel = "TTS threads",
                onFirstChange = { value -> onUpdateSettings { it.copy(ttsThreadCount = value.filter(Char::isDigit)) } },
                secondValue = state.settings.speakerId,
                secondLabel = "Speaker ID",
                onSecondChange = { value -> onUpdateSettings { it.copy(speakerId = value.filter(Char::isDigit)) } },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun VoiceModelPicker(
    label: String,
    models: List<VoiceModelOption>,
    selectedId: ModelId?,
    enabled: Boolean,
    onSelect: (ModelId) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = models.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled && models.isNotEmpty()) {
            Text(selected?.displayName ?: "Install a compatible model in Models")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (model.installed) {
                                "${model.displayName} (${model.languages.joinToString()})"
                            } else {
                                "${model.displayName} · Download in Models"
                            },
                        )
                    },
                    onClick = { onSelect(model.id); expanded = false },
                    enabled = model.installed,
                )
            }
        }
    }
}

@Composable
private fun VoiceSettingRow(
    firstValue: String,
    firstLabel: String,
    onFirstChange: (String) -> Unit,
    secondValue: String,
    secondLabel: String,
    onSecondChange: (String) -> Unit,
    enabled: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = firstValue,
            onValueChange = onFirstChange,
            enabled = enabled,
            label = { Text(firstLabel) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = secondValue,
            onValueChange = onSecondChange,
            enabled = enabled,
            label = { Text(secondLabel) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun Long.toReadableBytes(): String = when {
    this >= 1_073_741_824L -> "%.2f GiB".format(toDouble() / 1_073_741_824L)
    this >= 1_048_576L -> "%.1f MiB".format(toDouble() / 1_048_576L)
    else -> "$this bytes"
}
