package com.dmitriim.localailab.feature.assistant.impl.presentation.ui.tts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.AssistantSettingsSection

@Composable
internal fun SpeakPlaybackSettings(
    draft: SpeechOutputSettings,
    enabled: Boolean,
    onChange: (SpeechOutputSettings) -> Unit,
) {
    Column {
        AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_playback))
        OutputField(
            label = stringResource(CoreUiR.string.assistant_settings_speech_rate),
            value = draft.speed,
            enabled = enabled,
            onChange = { onChange(draft.copy(speed = it)) },
        )
        OutputField(
            label = stringResource(CoreUiR.string.assistant_settings_volume),
            value = draft.volume,
            enabled = enabled,
            onChange = { onChange(draft.copy(volume = it)) },
        )
        OutputField(
            label = stringResource(CoreUiR.string.assistant_settings_sentence_silence),
            value = draft.sentenceSilenceScale,
            enabled = enabled,
            onChange = { onChange(draft.copy(sentenceSilenceScale = it)) },
        )
        AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_performance))
        OutputField(
            label = stringResource(CoreUiR.string.assistant_settings_thread_count),
            value = draft.threadCount,
            enabled = enabled,
            onChange = { onChange(draft.copy(threadCount = it.filter(Char::isDigit))) },
        )
    }
}

@Composable
internal fun VoicePicker(
    voices: List<Pair<String, String>>,
    selectedVoiceId: String?,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = voices.firstOrNull { it.first == selectedVoiceId }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_25),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        )
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && voices.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected?.second ?: stringResource(CoreUiR.string.assistant_no_compatible_voice))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            voices.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun OutputField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
    )
}
