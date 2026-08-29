package com.dmitriim.localailab.feature.stt.presentation.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.AppSectionCard
import com.dmitriim.localailab.core.ui.component.AppSurfaceTone
import com.dmitriim.localailab.core.ui.component.StatusMessage
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.core.ui.style.AppFilterChipDefaults
import com.dmitriim.localailab.core.ui.text.asString
import com.dmitriim.localailab.feature.stt.presentation.SpeechToTextUiState
import com.dmitriim.localailab.feature.stt.presentation.SttLanguage
import com.dmitriim.localailab.feature.stt.presentation.SttOperation

@Composable
fun SpeechToTextScreen(
    state: SpeechToTextUiState,
    onSelectModel: (ModelId) -> Unit,
    onSelectLanguage: (SttLanguage) -> Unit,
    onThreadCountChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onImportAudio: () -> Unit,
    onRepeat: () -> Unit,
    onProfile: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val busy = state.operation != SttOperation.IDLE
    val systemNavigationPadding = if (dimensions.bottomNavigationOverlayClearance == 0.dp) {
        Modifier.navigationBarsPadding()
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(systemNavigationPadding)
            .verticalScroll(scroll)
            .padding(
                start = dimensions.screenPadding,
                top = dimensions.topBarOverlayClearance + 50.dp,
                end = dimensions.screenPadding,
                bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        SpeechToTextSetup(state, busy, onSelectModel, onSelectLanguage, onThreadCountChange)
        SpeechToTextAudioInput(state, busy, onStartRecording, onStopRecording, onImportAudio, onRepeat, onProfile, onCancel, onClear)
        SpeechToTextStatus(state)
        SpeechToTextTranscript(state.transcript, clipboard, context, onClear)
        state.metrics?.let { metrics ->
            SttRunMetricsCard(
                metrics = metrics,
                streamingModel = state.selectedModel?.recognitionMode ==
                    com.dmitriim.localailab.core.model.manifest.SttRecognitionMode.STREAMING,
            )
        }
    }
}

@Composable
private fun SpeechToTextSetup(state: SpeechToTextUiState, busy: Boolean, onSelectModel: (ModelId) -> Unit, onSelectLanguage: (SttLanguage) -> Unit, onThreadCountChange: (String) -> Unit) {
    AppSectionCard("Setup", tone = AppSurfaceTone.TONAL) {
        SpeechModelPicker(state.models, state.selectedModelId, enabled = !busy, onSelectModel)
        Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_133), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.availableLanguages.size) { index ->
                val language = state.availableLanguages[index]
                FilterChip(selected = state.language == language, onClick = { onSelectLanguage(language) }, enabled = !busy, label = { Text(language.label) }, colors = AppFilterChipDefaults.colors())
            }
        }
        OutlinedTextField(value = state.threadCount, onValueChange = onThreadCountChange, enabled = !busy, label = { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_134)) }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 64.dp))
    }
}

@Composable
private fun SpeechToTextAudioInput(state: SpeechToTextUiState, busy: Boolean, onStart: () -> Unit, onStop: () -> Unit, onImport: () -> Unit, onRepeat: () -> Unit, onProfile: () -> Unit, onCancel: () -> Unit, onClear: () -> Unit) {
    AppSectionCard("Audio input", tone = AppSurfaceTone.TONAL) {
        RecordingControls(state.operation, state.input != null, onStart, onStop, onImport, onRepeat, onCancel, onClear)
        state.level?.let { level ->
            Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_format_13, formatDuration(level.elapsedMs)), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_format_14, (level.rms * 100).toInt(), (level.peak * 100).toInt()), style = MaterialTheme.typography.bodyMedium)
        }
        state.input?.let { input ->
            Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_format_15, input.displayName, formatDuration(input.durationMs), input.sourceDescription), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onProfile, enabled = !busy && state.input != null && state.selectedModel?.installed == true, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_135))
        }
    }
}

@Composable
private fun SpeechToTextStatus(state: SpeechToTextUiState) {
    state.errorMessage?.let { StatusMessage(stringResource(CoreUiR.string.ui_copy_68), it.asString()) }
    if (state.operation != SttOperation.TRANSCRIBING) return
    val androidRecognizer = state.selectedModel?.engineId?.value == "android-speech-recognizer"
    StatusMessage(
        title = stringResource(if (androidRecognizer) CoreUiR.string.stt_processing_recording else CoreUiR.string.stt_transcribing_locally),
        explanation = if (androidRecognizer) {
            "Recording has stopped. Android SpeechRecognizer is receiving the captured audio in real time, so this takes about as long as the recording."
        } else {
            "${state.selectedModel?.displayName.orEmpty()} is processing bounded 30-second audio segments. You can cancel at any time."
        },
    )
}

@Composable
private fun SpeechToTextTranscript(transcript: String, clipboard: androidx.compose.ui.platform.ClipboardManager, context: android.content.Context, onClear: () -> Unit) {
    if (transcript.isBlank()) return
    AppSectionCard("Final transcript", tone = AppSurfaceTone.TONAL) {
        Text(transcript, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.SansSerif)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
            TextButton(onClick = { clipboard.setText(AnnotatedString(transcript)) }) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_136)) }
            TextButton(onClick = { shareTranscript(context, transcript) }) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_137)) }
            TextButton(onClick = onClear, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_138)) }
        }
    }
}

private fun shareTranscript(context: android.content.Context, transcript: String) {
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, transcript)
            },
            "Share transcript",
        ),
    )
}

@Composable
private fun RecordingControls(
    operation: SttOperation,
    hasInput: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onImport: () -> Unit,
    onRepeat: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    when (operation) {
        SttOperation.RECORDING -> Button(onClick = onStop) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_139)) }
        SttOperation.STOPPING -> OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_140)) }
        SttOperation.IMPORTING -> {
            OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_141)) }
            TextButton(onClick = onCancel) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_142)) }
        }
        SttOperation.TRANSCRIBING -> {
            OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_143)) }
            TextButton(onClick = onCancel) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_144)) }
        }
        SttOperation.CANCELLING -> OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_145)) }
        SttOperation.IDLE -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                ) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_146), maxLines = 1, softWrap = false) }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onImport,
                ) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_147), maxLines = 1, softWrap = false) }
            }
            if (hasInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onRepeat,
                    ) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_148), maxLines = 1, softWrap = false) }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onClear,
                    ) { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_149), maxLines = 1, softWrap = false) }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
