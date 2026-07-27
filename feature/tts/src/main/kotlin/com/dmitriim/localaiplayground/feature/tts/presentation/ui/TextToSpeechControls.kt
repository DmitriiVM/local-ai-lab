package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.feature.tts.presentation.TextToSpeechUiState
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsLanguage
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsModelOption
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsOperation
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsVoiceOption

@Composable
internal fun TextToSpeechModelPicker(
    models: List<TtsModelOption>,
    selectedId: ModelId?,
    enabled: Boolean,
    onSelect: (ModelId) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = models.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Voice model", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled && models.isNotEmpty()) {
            Text(selected?.displayName ?: "Install Supertonic 3 INT8 in Models")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text("${model.displayName} (${model.languages.joinToString()})") },
                    onClick = { onSelect(model.id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TextToSpeechVoiceSelector(
    visible: Boolean,
    voices: List<TtsVoiceOption>,
    selectedId: String?,
    language: TtsLanguage,
    enabled: Boolean,
    operation: TtsOperation,
    previewVoiceId: String?,
    hasPreviewText: Boolean,
    onSelect: (String) -> Unit,
    onPreview: (String) -> Unit,
    onStopPreview: () -> Unit,
) {
    if (!visible) return
    var sheetVisible by remember { mutableStateOf(false) }
    val selected = voices.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Voice", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { sheetVisible = true },
            enabled = enabled && selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected?.displayName ?: "No voice available for ${language.label}")
        }
    }
    if (sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                if (operation in setOf(TtsOperation.PREVIEWING, TtsOperation.CANCELLING)) {
                    onStopPreview()
                }
                sheetVisible = false
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "Choose a voice",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Text(
                    text = "Play previews the current text without changing the selected voice.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp),
                ) {
                    items(voices, key = TtsVoiceOption::id) { voice ->
                        val previewingThisVoice =
                            previewVoiceId == voice.id &&
                                operation in setOf(TtsOperation.PREVIEWING, TtsOperation.CANCELLING)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) {
                                    onSelect(voice.id)
                                    sheetVisible = false
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedId == voice.id,
                                onClick = null,
                                enabled = enabled,
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = voice.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                voice.description?.takeIf(String::isNotBlank)?.let { description ->
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    if (previewingThisVoice) {
                                        onStopPreview()
                                    } else {
                                        onPreview(voice.id)
                                    }
                                },
                                enabled = hasPreviewText &&
                                    operation !in setOf(
                                        TtsOperation.SYNTHESIZING,
                                        TtsOperation.CANCELLING,
                                    ),
                            ) {
                                Text(
                                    when {
                                        previewingThisVoice &&
                                            operation == TtsOperation.CANCELLING -> "Stopping…"
                                        previewingThisVoice -> "Stop"
                                        else -> "Play"
                                    },
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
internal fun TextToSpeechLanguageControls(
    selectedLanguage: TtsLanguage,
    enabled: Boolean,
    onSelectLanguage: (TtsLanguage) -> Unit,
    onApplySample: (TtsLanguage) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TtsLanguage.entries.forEach { language ->
            FilterChip(
                selected = selectedLanguage == language,
                onClick = { onSelectLanguage(language) },
                enabled = enabled,
                label = { Text(language.label) },
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TtsLanguage.entries.forEach { language ->
            TextButton(onClick = { onApplySample(language) }, enabled = enabled) {
                Text("${language.label} sample")
            }
        }
    }
}

@Composable
internal fun TextToSpeechSettings(
    state: TextToSpeechUiState,
    enabled: Boolean,
    onSpeedChange: (Float) -> Unit,
    onSentenceSilenceChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onThreadCountChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Supported controls", style = MaterialTheme.typography.titleMedium)
            TextToSpeechParameterSlider("Speech rate", state.speed, "%.2f×".format(state.speed), 0.5f..2f, enabled, onSpeedChange)
            TextToSpeechParameterSlider("Sentence silence", state.sentenceSilenceScale, "%.2f×".format(state.sentenceSilenceScale), 0f..2f, enabled, onSentenceSilenceChange)
            TextToSpeechParameterSlider("Playback volume", state.volume, "${(state.volume * 100).toInt()}%", 0f..1f, enabled, onVolumeChange)
            OutlinedTextField(
                value = state.threadCount,
                onValueChange = onThreadCountChange,
                enabled = enabled,
                label = { Text("CPU threads (0 = safe default)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun TextToSpeechAudioEffectsSettings(
    state: TextToSpeechUiState,
    enabled: Boolean,
    onPitchChange: (Float) -> Unit,
    onFormantChange: (Float) -> Unit,
    onLowEqChange: (Float) -> Unit,
    onMidEqChange: (Float) -> Unit,
    onHighEqChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    val effects = state.audioEffects
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Post-processing", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onReset, enabled = enabled && !effects.isNeutral) {
                    Text("Reset")
                }
            }
            Text(
                "These effects are applied to the generated PCM before playback, replay, sharing, and WAV export.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextToSpeechParameterSlider(
                "Pitch",
                effects.pitchSemitones,
                effects.pitchSemitones.signed("st"),
                SpeechAudioEffects.PITCH_RANGE,
                enabled,
                onPitchChange,
            )
            TextToSpeechParameterSlider(
                "Formant",
                effects.formantSemitones,
                effects.formantSemitones.signed("st"),
                SpeechAudioEffects.FORMANT_RANGE,
                enabled,
                onFormantChange,
            )
            Text("Equalizer", style = MaterialTheme.typography.labelLarge)
            TextToSpeechParameterSlider(
                "Low · 160 Hz",
                effects.lowEqDb,
                effects.lowEqDb.signed("dB"),
                SpeechAudioEffects.EQ_RANGE,
                enabled,
                onLowEqChange,
            )
            TextToSpeechParameterSlider(
                "Presence · 1.5 kHz",
                effects.midEqDb,
                effects.midEqDb.signed("dB"),
                SpeechAudioEffects.EQ_RANGE,
                enabled,
                onMidEqChange,
            )
            TextToSpeechParameterSlider(
                "High · 5 kHz",
                effects.highEqDb,
                effects.highEqDb.signed("dB"),
                SpeechAudioEffects.EQ_RANGE,
                enabled,
                onHighEqChange,
            )
            TextToSpeechParameterSlider(
                "Saturation drive",
                effects.saturationDriveDb,
                "%.1f dB".format(effects.saturationDriveDb),
                SpeechAudioEffects.SATURATION_RANGE,
                enabled,
                onSaturationChange,
            )
        }
    }
}

@Composable
internal fun TextToSpeechPlaybackControls(
    state: TextToSpeechUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            when {
                state.operation == TtsOperation.PREVIEWING -> Unit
                state.operation in setOf(TtsOperation.SYNTHESIZING, TtsOperation.CANCELLING) -> {
                    Button(onClick = onStop, enabled = state.operation != TtsOperation.CANCELLING) {
                        Text(if (state.operation == TtsOperation.CANCELLING) "Stopping…" else "Stop")
                    }
                }
                state.playback.status == SpeechPlaybackStatus.PLAYING -> {
                    Button(onClick = onPause) { Text("Pause") }
                    OutlinedButton(onClick = onStop) { Text("Stop") }
                }
                state.playback.status == SpeechPlaybackStatus.PAUSED -> {
                    Button(onClick = onResume) { Text("Resume") }
                    OutlinedButton(onClick = onStop) { Text("Stop") }
                }
                else -> if (state.output != null) OutlinedButton(onClick = onReplay) { Text("Replay") }
            }
        }
        if (state.output != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport) { Text("Export WAV") }
                OutlinedButton(onClick = onShare) { Text("Share") }
            }
        }
    }
}

@Composable
private fun TextToSpeechParameterSlider(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(valueText, fontFamily = FontFamily.Monospace)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, enabled = enabled)
    }
}

private fun Float.signed(unit: String): String = "%+.1f %s".format(this, unit)
