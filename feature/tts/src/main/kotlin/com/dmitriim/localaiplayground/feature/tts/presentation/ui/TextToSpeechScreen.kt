package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.tts.presentation.TextToSpeechUiState
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsLanguage
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsOperation

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
                start = 16.dp,
                top = dimensions.topBarOverlayClearance + 12.dp,
                end = 16.dp,
                bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("On-device text to speech", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Choose Android’s on-device speech engine or an installed local model. Generated audio can be replayed, processed, shared, and exported as WAV.",
            style = MaterialTheme.typography.bodyMedium,
        )

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
            Text(
                "Expressive tags: [laugh], [chuckle], [cough], [sigh], [whispering], [happy], [angry], [dramatic], and more.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Generated output: Not watermarked",
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

        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChange,
            enabled = !busy,
            label = { Text("Text to synthesize") },
            supportingText = { Text("${state.text.length} / ${state.characterLimit} characters") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onSynthesize,
            enabled = !busy && state.selectedVoice != null && state.text.isNotBlank(),
        ) {
            Text("Synthesize & play")
        }

        TextToSpeechLanguageControls(
            state.language,
            !busy,
            onSelectLanguage,
            onApplySample,
            englishOnly = state.usesReferenceVoice,
        )
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
            StatusMessage(title = "Text to speech", explanation = it)
        }
        state.errorMessage?.let {
            StatusMessage(title = "Text to speech needs attention", explanation = it)
        }
    }
}
