package com.dmitriim.localailab.feature.tts.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.AppSectionCard
import com.dmitriim.localailab.core.ui.component.AppSurfaceTone
import com.dmitriim.localailab.core.ui.component.StatusMessage
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.core.ui.text.asString
import com.dmitriim.localailab.feature.tts.impl.presentation.TextToSpeechUiState
import com.dmitriim.localailab.feature.tts.impl.presentation.TtsLanguage
import com.dmitriim.localailab.feature.tts.impl.presentation.TtsOperation

@Composable
internal fun TextToSpeechScreen(
    state: TextToSpeechUiState,
    onSelectModel: (ModelId) -> Unit,
    onSelectVoice: (String) -> Unit,
    onPreviewVoice: (String) -> Unit,
    activityCallbacks: TextToSpeechActivityCallbacks,
    onStopReferenceRecording: () -> Unit,
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
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current
    val activityActions = rememberTextToSpeechActivityActions(activityCallbacks)
    val busy = state.isBusy()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = dimensions.screenPadding,
                top = dimensions.topBarOverlayClearance + 50.dp,
                end = dimensions.screenPadding,
                bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        TtsSetupSection(
            state = state,
            enabled = !busy,
            onSelectModel = onSelectModel,
            onSelectVoice = onSelectVoice,
            onPreviewVoice = onPreviewVoice,
            onRecordReference = activityActions.requestReferenceRecording,
            onStopReferenceRecording = onStopReferenceRecording,
            onImportReference = activityActions.selectReferenceAudio,
            onDeleteReference = onDeleteReference,
            onSelectLanguage = onSelectLanguage,
            onApplySample = onApplySample,
            onStop = onStop,
        )
        TtsComposeSection(
            state = state,
            enabled = !busy,
            onTextChange = onTextChange,
            onSynthesize = onSynthesize,
            onProfile = onProfile,
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
            onExport = { activityActions.createExportDocument(state.output?.displayName ?: "local-ai-speech.wav") },
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

@Composable
private fun TtsSetupSection(
    state: TextToSpeechUiState,
    enabled: Boolean,
    onSelectModel: (ModelId) -> Unit,
    onSelectVoice: (String) -> Unit,
    onPreviewVoice: (String) -> Unit,
    onRecordReference: () -> Unit,
    onStopReferenceRecording: () -> Unit,
    onImportReference: () -> Unit,
    onDeleteReference: (String) -> Unit,
    onSelectLanguage: (TtsLanguage) -> Unit,
    onApplySample: (TtsLanguage) -> Unit,
    onStop: () -> Unit,
) {
    var consentAction by remember { mutableStateOf<ReferenceConsentAction?>(null) }
    AppSectionCard("Setup", tone = AppSurfaceTone.TONAL) {
        TextToSpeechModelPicker(state.models, state.selectedModelId, enabled, onSelectModel)
        if (state.usesReferenceVoice) {
            ChatterboxReferenceVoiceSelector(
                state = state,
                enabled = enabled,
                onSelect = onSelectVoice,
                onRecord = { consentAction = ReferenceConsentAction.RECORD },
                onStopRecording = onStopReferenceRecording,
                onImport = { consentAction = ReferenceConsentAction.IMPORT },
                onDelete = onDeleteReference,
            )
            Text(
                text = stringResource(CoreUiR.string.tts_text_to_speech_screen_179),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(CoreUiR.string.tts_text_to_speech_screen_180),
                style = MaterialTheme.typography.labelLarge,
            )
        } else {
            TextToSpeechVoiceSelector(
                state.selectedModel?.voices?.size?.let { it > 1 } == true,
                state.compatibleVoices, state.selectedVoiceId, state.language, enabled,
                state.operation, state.previewVoiceId, state.text.isNotBlank(),
                onSelectVoice, onPreviewVoice, onStop,
            )
        }
        Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_181), style = MaterialTheme.typography.titleSmall)
        TextToSpeechLanguageControls(
            state.language,
            enabled,
            onSelectLanguage,
            onApplySample,
            englishOnly = state.usesReferenceVoice,
        )
        ReferenceConsentDialog(
            action = consentAction,
            onDismiss = { consentAction = null },
            onConfirm = { action ->
                consentAction = null
                when (action) {
                    ReferenceConsentAction.RECORD -> onRecordReference()
                    ReferenceConsentAction.IMPORT -> onImportReference()
                }
            },
        )
    }
}

@Composable
private fun ReferenceConsentDialog(
    action: ReferenceConsentAction?,
    onDismiss: () -> Unit,
    onConfirm: (ReferenceConsentAction) -> Unit,
) {
    action?.let {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(CoreUiR.string.tts_text_to_speech_route_151)) },
            text = {
                Text(
                    stringResource(CoreUiR.string.tts_text_to_speech_route_152),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(it) },
                ) { Text(stringResource(CoreUiR.string.tts_text_to_speech_route_153)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.tts_text_to_speech_route_154)) }
            },
        )
    }
}

private enum class ReferenceConsentAction { RECORD, IMPORT }

private fun TextToSpeechUiState.isBusy(): Boolean = operation != TtsOperation.IDLE ||
    playback.status in setOf(
        SpeechPlaybackStatus.READY,
        SpeechPlaybackStatus.PLAYING,
        SpeechPlaybackStatus.PAUSED,
    )

@Composable
private fun TtsComposeSection(
    state: TextToSpeechUiState,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSynthesize: () -> Unit,
    onProfile: () -> Unit,
) {
    AppSectionCard("Compose", tone = AppSurfaceTone.TONAL) {
        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChange,
            enabled = enabled,
            label = { Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_182)) },
            supportingText = {
                Text(
                    stringResource(
                        CoreUiR.string.tts_text_to_speech_screen_format_17,
                        state.text.length,
                        state.characterLimit,
                    ),
                )
            },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        val canGenerate = enabled && state.selectedVoice != null && state.text.isNotBlank()
        Button(onClick = onSynthesize, enabled = canGenerate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_183))
        }
        OutlinedButton(onClick = onProfile, enabled = canGenerate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(CoreUiR.string.tts_text_to_speech_screen_184))
        }
    }
}
