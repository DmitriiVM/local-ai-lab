package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR
import com.dmitriim.localaiplayground.feature.tts.presentation.TextToSpeechUiState

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
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CollapsibleTtsSettingsHeader(
                stringResource(CoreUiR.string.ui_copy_81),
                supportedControlsSummary(state),
                expanded,
            ) { expanded = !expanded }
            if (expanded) {
                if (state.supportsSpeechRate) {
                    TextToSpeechParameterSlider(
                        "Speech rate",
                        state.speed,
                        "%.2f×".format(state.speed),
                        0.5f..2f,
                        enabled,
                        onSpeedChange,
                    )
                }
                if (state.supportsSentenceSilence) {
                    TextToSpeechParameterSlider(
                        "Sentence silence",
                        state.sentenceSilenceScale,
                        "%.2f×".format(state.sentenceSilenceScale),
                        0.01f..1f,
                        enabled,
                        onSentenceSilenceChange,
                    )
                }
                TextToSpeechParameterSlider(
                    "Playback volume",
                    state.volume,
                    "${(state.volume * 100).toInt()}%",
                    0f..1f,
                    enabled,
                    onVolumeChange,
                )
                if (!state.usesPlatformVoice) {
                    OutlinedTextField(
                        value = state.threadCount,
                        onValueChange = onThreadCountChange,
                        enabled = enabled,
                        label = {
                            Text(
                                stringResource(
                                    CoreUiR.string.tts_text_to_speech_controls_164,
                                ),
                            )
                        },
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
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CollapsibleTtsSettingsHeader(
                stringResource(CoreUiR.string.ui_copy_82),
                if (effects.isNeutral) {
                    "No effects applied"
                } else {
                    "Custom effects applied"
                },
                expanded,
            ) { expanded = !expanded }
            if (expanded) {
                TextToSpeechAudioEffectsContent(
                    effects = effects,
                    enabled = enabled,
                    onPitchChange = onPitchChange,
                    onFormantChange = onFormantChange,
                    onLowEqChange = onLowEqChange,
                    onMidEqChange = onMidEqChange,
                    onHighEqChange = onHighEqChange,
                    onSaturationChange = onSaturationChange,
                    onReset = onReset,
                )
            }
        }
    }
}

@Composable
private fun TextToSpeechAudioEffectsContent(
    effects: SpeechAudioEffects,
    enabled: Boolean,
    onPitchChange: (Float) -> Unit,
    onFormantChange: (Float) -> Unit,
    onLowEqChange: (Float) -> Unit,
    onMidEqChange: (Float) -> Unit,
    onHighEqChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = onReset,
            enabled = enabled && !effects.isNeutral,
        ) {
            Text(
                stringResource(
                    CoreUiR.string.tts_text_to_speech_controls_165,
                ),
            )
        }
    }
    Text(
        stringResource(
            CoreUiR.string.tts_text_to_speech_controls_166,
        ),
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
    Text(
        stringResource(
            CoreUiR.string.tts_text_to_speech_controls_167,
        ),
        style = MaterialTheme.typography.labelLarge,
    )
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onToggle) {
            Text(
                stringResource(
                    if (expanded) {
                        CoreUiR.string.core_ui_hide
                    } else {
                        CoreUiR.string.core_ui_show
                    },
                ),
            )
        }
    }
}

private fun supportedControlsSummary(
    state: TextToSpeechUiState,
): String = buildList {
    state.supportsSpeechRate.let {
        if (it) add("Rate ${"%.2f×".format(state.speed)}")
    }
    state.supportsSentenceSilence.let {
        if (it) add("Silence ${"%.2f×".format(state.sentenceSilenceScale)}")
    }
    add("Volume ${(state.volume * 100).toInt()}%")
    if (!state.usesPlatformVoice) add("${state.threadCount} threads")
}.joinToString(" · ")

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(valueText, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
        )
    }
}

private fun Float.signed(unit: String): String = "%+.1f %s".format(this, unit)
