package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.core.ui.component.AppSectionCard
import com.dmitriim.localaiplayground.core.ui.component.AppSurfaceTone
import com.dmitriim.localaiplayground.core.ui.layout.LocalAppDimensions
import com.dmitriim.localaiplayground.feature.tts.presentation.TextToSpeechUiState
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsLanguage
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsOperation
import androidx.compose.ui.res.stringResource
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR
import com.dmitriim.localaiplayground.core.ui.text.asString

@Composable
fun TextToSpeechScreen(
    state: TextToSpeechUiState,
    onSelectModel: (ModelId) -> Unit,
    onSelectVoice: (String) -> Unit,
    onPreviewVoice: (String) -> Unit,
    onRecordReference: () -> Unit,
    onStopReferenceRecording: () -> Unit,
    onImportReference: () -> Unit,
    onDeleteReference: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onSelectLanguage: (TtsLanguage) -> Unit,
    onApplySample: (TtsLanguage) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSentenceSilenceChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onThreadCountChange: (String) -> Unit,
    onPitchChange: (Float) -> Unit,
    onFormantChange: (Float) -> Unit,
    onLowEqChange: (Float) -> Unit,
    onMidEqChange: (Float) -> Unit,
    onHighEqChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onResetAudioEffects: () -> Unit,
    onSynthesize: () -> Unit,
    onProfile: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val busy = state.operation != TtsOperation.IDLE ||
        state.playback.status in setOf(
            SpeechPlaybackStatus.READY,
            SpeechPlaybackStatus.PLAYING,
            SpeechPlaybackStatus.PAUSED,
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = dimensions.screenPadding,
                top = dimensions.topBarOverlayClearance + 44.dp,
                end = dimensions.screenPadding,
                bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_177), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_178),
            style = MaterialTheme.typography.bodyMedium,
        )

        AppSectionCard("Setup", tone = AppSurfaceTone.TONAL) {
            TextToSpeechModelPicker(state.models, state.selectedModelId, !busy, onSelectModel)
            if (state.usesReferenceVoice) {
                ChatterboxReferenceVoiceSelector(
                    state = state,
                    enabled = !busy,
                    onSelect = onSelectVoice,
                    onRecord = onRecordReference,
                    onStopRecording = onStopReferenceRecording,
                    onImport = onImportReference,
                    onDelete = onDeleteReference,
                )
                Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_179),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_180),
                    style = MaterialTheme.typography.labelLarge,
                )
            } else {
                TextToSpeechVoiceSelector(
                    visible = state.selectedModel?.voices?.size?.let { it > 1 } == true,
                    voices = state.compatibleVoices,
                    selectedId = state.selectedVoiceId,
                    language = state.language,
                    enabled = !busy,
                    operation = state.operation,
                    previewVoiceId = state.previewVoiceId,
                    hasPreviewText = state.text.isNotBlank(),
                    onSelect = onSelectVoice,
                    onPreview = onPreviewVoice,
                    onStopPreview = onStop,
                )
            }
            Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_181), style = MaterialTheme.typography.titleSmall)
            TextToSpeechLanguageControls(
                state.language,
                !busy,
                onSelectLanguage,
                onApplySample,
                englishOnly = state.usesReferenceVoice,
            )
        }

        AppSectionCard("Compose", tone = AppSurfaceTone.TONAL) {
            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                enabled = !busy,
                label = { Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_182)) },
                supportingText = { Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_format_17, state.text.length, state.characterLimit)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSynthesize,
                enabled = !busy && state.selectedVoice != null && state.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_183))
            }
            OutlinedButton(
                onClick = onProfile,
                enabled = !busy && state.selectedVoice != null && state.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_184))
            }
        }
        TextToSpeechSettings(
            state = state,
            enabled = !busy,
            onSpeedChange = onSpeedChange,
            onSentenceSilenceChange = onSentenceSilenceChange,
            onVolumeChange = onVolumeChange,
            onThreadCountChange = onThreadCountChange,
        )
        TextToSpeechAudioEffectsSettings(
            state = state,
            enabled = !busy,
            onPitchChange = onPitchChange,
            onFormantChange = onFormantChange,
            onLowEqChange = onLowEqChange,
            onMidEqChange = onMidEqChange,
            onHighEqChange = onHighEqChange,
            onSaturationChange = onSaturationChange,
            onReset = onResetAudioEffects,
        )
        TextToSpeechPlaybackControls(
            state = state,
            onPause = onPause,
            onResume = onResume,
            onStop = onStop,
            onReplay = onReplay,
            onExport = onExport,
            onShare = onShare,
        )

        TextToSpeechPlaybackStatus(state.playback)
        state.output?.let { GeneratedAudioCard(it) }
        state.metrics?.let { TextToSpeechMetricsCard(it) }
        state.statusMessage?.let {
            StatusMessage(title = stringResource(CoreUiR.string.ui_copy_101), explanation = it.asString())
        }
        state.errorMessage?.let {
            StatusMessage(title = stringResource(CoreUiR.string.ui_copy_102), explanation = it.asString())
        }
    }
}
