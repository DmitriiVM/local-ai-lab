package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localaiplayground.core.ui.style.AppFilterChipDefaults
import com.dmitriim.localaiplayground.feature.tts.presentation.TextToSpeechUiState
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsLanguage
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsOperation
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsVoiceOption
import androidx.compose.ui.res.stringResource
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR

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
        Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_155), style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { sheetVisible = true },
            enabled = enabled && selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected?.displayName ?: stringResource(CoreUiR.string.tts_no_voice_for_language, language.label))
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
                    text = stringResource(CoreUiR.string.ui_copy_78),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Text(
                    text = stringResource(CoreUiR.string.ui_copy_79),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp, end = 24.dp),
                )
                Text(
                    text = stringResource(CoreUiR.string.ui_copy_80),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 8.dp),
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
                        val selected = selectedId == voice.id
                        val colors = MaterialTheme.colorScheme
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable(enabled = enabled) {
                                    onSelect(voice.id)
                                    sheetVisible = false
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) {
                                colors.tertiaryContainer.copy(alpha = 0.38f)
                            } else {
                                colors.surfaceContainerHigh.copy(alpha = 0.44f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) {
                                    colors.tertiary.copy(alpha = 0.58f)
                                } else {
                                    colors.outlineVariant.copy(alpha = 0.30f)
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    enabled = enabled,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.tertiary,
                                        unselectedColor = colors.outline,
                                    ),
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = voice.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) colors.tertiary else colors.onSurface,
                                    )
                                    voice.description?.takeIf(String::isNotBlank)?.let { description ->
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (selected) {
                                                colors.onTertiaryContainer.copy(alpha = 0.82f)
                                            } else {
                                                colors.onSurfaceVariant
                                            },
                                        )
                                    }
                                }
                                val previewEnabled = hasPreviewText &&
                                    operation !in setOf(
                                        TtsOperation.SYNTHESIZING,
                                        TtsOperation.CANCELLING,
                                    )
                                OutlinedIconButton(
                                    onClick = {
                                        if (previewingThisVoice) {
                                            onStopPreview()
                                        } else {
                                            onPreview(voice.id)
                                        }
                                    },
                                    enabled = previewEnabled,
                                    border = BorderStroke(
                                        1.dp,
                                        if (previewEnabled) {
                                            colors.tertiary.copy(alpha = 0.64f)
                                        } else {
                                            colors.outlineVariant.copy(alpha = 0.32f)
                                        },
                                    ),
                                    colors = IconButtonDefaults.outlinedIconButtonColors(
                                        contentColor = colors.tertiary,
                                        disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
                                    ),
                                ) {
                                    Icon(
                                        imageVector = if (previewingThisVoice) {
                                            Icons.Outlined.Stop
                                        } else {
                                            Icons.Outlined.PlayArrow
                                        },
                                        contentDescription = if (previewingThisVoice) {
                                            "Stop preview for ${voice.displayName}"
                                        } else {
                                            "Play preview for ${voice.displayName}"
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatterboxReferenceVoiceSelector(
    state: TextToSpeechUiState,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onRecord: () -> Unit,
    onStopRecording: () -> Unit,
    onImport: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var sheetVisible by remember { mutableStateOf(false) }
    val selected = state.selectedVoice
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f))
        Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_156), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_157),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.operation in setOf(TtsOperation.RECORDING_REFERENCE, TtsOperation.STOPPING_REFERENCE)) {
            val elapsed = state.referenceLevel?.elapsedMs ?: 0L
            Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_158).format(elapsed / 1_000.0))
            Button(
                onClick = onStopRecording,
                enabled = state.operation == TtsOperation.RECORDING_REFERENCE &&
                    elapsed >= 5_000,
            ) {
                Text(
                    stringResource(
                        if (elapsed < 5_000) CoreUiR.string.tts_keep_recording else CoreUiR.string.tts_save_reference,
                    ),
                )
            }
        } else {
            OutlinedButton(
                onClick = { sheetVisible = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selected?.displayName ?: stringResource(CoreUiR.string.tts_choose_saved_reference))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRecord,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                ) {
                    Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_159))
                }
                OutlinedButton(
                    onClick = onImport,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_160))
                }
            }
        }
    }
    if (sheetVisible) {
        ModalBottomSheet(onDismissRequest = { sheetVisible = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_161),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                if (state.compatibleVoices.isEmpty()) {
                    Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_162),
                        modifier = Modifier.padding(24.dp),
                    )
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                    items(state.compatibleVoices, key = TtsVoiceOption::id) { voice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) {
                                    onSelect(voice.id)
                                    sheetVisible = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.selectedVoiceId == voice.id, onClick = null)
                            Column(Modifier.weight(1f)) {
                                Text(voice.displayName)
                                voice.description?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            TextButton(
                                onClick = { onDelete(voice.id) },
                                enabled = enabled,
                            ) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_163)) }
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
    englishOnly: Boolean = false,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TtsLanguage.entries) { language ->
            FilterChip(
                selected = selectedLanguage == language,
                onClick = { onSelectLanguage(language) },
                // Chatterbox Turbo Q4 is English-only.
                enabled = enabled && (!englishOnly || language == TtsLanguage.ENGLISH),
                label = { Text(language.label) },
                colors = AppFilterChipDefaults.colors(),
            )
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(TtsLanguage.entries) { language ->
            TextButton(
                onClick = { onApplySample(language) },
                enabled = enabled && (!englishOnly || language == TtsLanguage.ENGLISH),
            ) {
                Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_format_16, language.label), maxLines = 1, softWrap = false)
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
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CollapsibleTtsSettingsHeader(
                title = stringResource(CoreUiR.string.ui_copy_81),
                summary = supportedControlsSummary(state),
                expanded = expanded,
                onToggle = { expanded = !expanded },
            )
            if (expanded) {
                if (state.supportsSpeechRate) {
                    TextToSpeechParameterSlider("Speech rate", state.speed, "%.2f×".format(state.speed), 0.5f..2f, enabled, onSpeedChange)
                }
                if (state.supportsSentenceSilence) {
                    TextToSpeechParameterSlider("Sentence silence", state.sentenceSilenceScale, "%.2f×".format(state.sentenceSilenceScale), 0f..2f, enabled, onSentenceSilenceChange)
                }
                TextToSpeechParameterSlider("Playback volume", state.volume, "${(state.volume * 100).toInt()}%", 0f..1f, enabled, onVolumeChange)
                if (!state.usesPlatformVoice) {
                    OutlinedTextField(
                        value = state.threadCount,
                        onValueChange = onThreadCountChange,
                        enabled = enabled,
                        label = { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_164)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CollapsibleTtsSettingsHeader(
                title = stringResource(CoreUiR.string.ui_copy_82),
                summary = if (effects.isNeutral) "No effects applied" else "Custom effects applied",
                expanded = expanded,
                onToggle = { expanded = !expanded },
            )
            if (expanded) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onReset, enabled = enabled && !effects.isNeutral) {
                        Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_165))
                    }
                }
                Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_166),
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
                Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_167), style = MaterialTheme.typography.labelLarge)
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
}

@Composable
private fun CollapsibleTtsSettingsHeader(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onToggle) {
            Text(stringResource(if (expanded) CoreUiR.string.core_ui_hide else CoreUiR.string.core_ui_show))
        }
    }
}

private fun supportedControlsSummary(state: TextToSpeechUiState): String = buildList {
    state.supportsSpeechRate.let { if (it) add("Rate ${"%.2f×".format(state.speed)}") }
    state.supportsSentenceSilence.let { if (it) add("Silence ${"%.2f×".format(state.sentenceSilenceScale)}") }
    add("Volume ${(state.volume * 100).toInt()}%")
    if (!state.usesPlatformVoice) add("${state.threadCount} threads")
}.joinToString(" · ")

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
                        Text(
                            stringResource(
                                if (state.operation == TtsOperation.CANCELLING) {
                                    CoreUiR.string.tts_stopping
                                } else {
                                    CoreUiR.string.tts_stop
                                },
                            ),
                        )
                    }
                }
                state.playback.status == SpeechPlaybackStatus.PLAYING -> {
                    Button(onClick = onPause) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_168)) }
                    OutlinedButton(onClick = onStop) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_169)) }
                }
                state.playback.status == SpeechPlaybackStatus.PAUSED -> {
                    Button(onClick = onResume) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_170)) }
                    OutlinedButton(onClick = onStop) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_171)) }
                }
                else -> if (state.output != null) OutlinedButton(onClick = onReplay) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_172)) }
            }
        }
        if (state.output != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_173)) }
                OutlinedButton(onClick = onShare) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_174)) }
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
