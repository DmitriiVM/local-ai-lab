package com.dmitriim.localaiplayground.feature.stt.presentation.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.stt.presentation.SpeechModelOption
import com.dmitriim.localaiplayground.feature.stt.presentation.SpeechToTextUiState
import com.dmitriim.localaiplayground.feature.stt.presentation.SttLanguage
import com.dmitriim.localaiplayground.feature.stt.presentation.SttOperation

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
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val busy = state.operation != SttOperation.IDLE
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(start = 16.dp, top = dimensions.topBarOverlayClearance + 12.dp, end = 16.dp, bottom = 24.dp + dimensions.bottomNavigationOverlayClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Local speech to text", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Choose the Android on-device recognizer or an installed speech model. Recording shows live levels, then final text appears after you stop.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SpeechModelPicker(state.models, state.selectedModelId, enabled = !busy, onSelectModel)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.availableLanguages.forEach { language ->
                FilterChip(
                    selected = state.language == language,
                    onClick = { onSelectLanguage(language) },
                    enabled = !busy,
                    label = { Text(language.label) },
                )
            }
        }
        OutlinedTextField(
            value = state.threadCount,
            onValueChange = onThreadCountChange,
            enabled = !busy,
            label = { Text("CPU threads (0 = safe default)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        RecordingControls(
            operation = state.operation,
            hasInput = state.input != null,
            onStart = onStartRecording,
            onStop = onStopRecording,
            onImport = onImportAudio,
            onRepeat = onRepeat,
            onCancel = onCancel,
            onClear = onClear,
        )
        state.level?.let { level ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Recording ${formatDuration(level.elapsedMs)}", style = MaterialTheme.typography.titleSmall)
                    Text("Live input level: ${(level.rms * 100).toInt()}% RMS · ${(level.peak * 100).toInt()}% peak")
                }
            }
        }
        state.input?.let { input ->
            Text("Input: ${input.displayName} · ${formatDuration(input.durationMs)} · ${input.sourceDescription}", style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let { StatusMessage(title = "Speech to text needs attention", explanation = it) }
        if (state.operation == SttOperation.TRANSCRIBING) {
            val isAndroidRecognizer = state.selectedModel?.engineId?.value == "android-speech-recognizer"
            StatusMessage(
                title = if (isAndroidRecognizer) "Processing recording" else "Transcribing locally",
                explanation = if (isAndroidRecognizer) {
                    "Recording has stopped. Android SpeechRecognizer is receiving the captured audio in real time, so this takes about as long as the recording."
                } else {
                    "${state.selectedModel?.displayName.orEmpty()} is processing bounded 30-second audio segments. You can cancel at any time."
                },
            )
        }
        if (state.transcript.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Final transcript", style = MaterialTheme.typography.titleMedium)
                    Text(state.transcript, fontFamily = FontFamily.SansSerif)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(state.transcript)) }) { Text("Copy") }
                        TextButton(onClick = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, state.transcript)
                                    },
                                    "Share transcript",
                                ),
                            )
                        }) { Text("Share") }
                        TextButton(onClick = onClear) { Text("Clear") }
                    }
                }
            }
        }
        state.metrics?.let { metrics ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Run metrics", style = MaterialTheme.typography.titleMedium)
                    Text("Audio: ${formatDuration(metrics.audioDurationMs)} · processing: ${formatDuration(metrics.processingDurationMs)}")
                    Text("First partial: — · final result: ${formatDuration(metrics.timeToFinalMs)}")
                    Text("RTF: ${metrics.realTimeFactor?.let { "%.2f".format(it) } ?: "—"}")
                    Text("Segments: ${metrics.segmentCount} · model load: ${formatDuration(metrics.loadDurationMs)} · threads: ${metrics.effectiveThreadCount}")
                    Text(
                        if (state.selectedModel?.recognitionMode == com.dmitriim.localaiplayground.core.model.manifest.SttRecognitionMode.STREAMING) {
                            "This model supports streaming; the current screen finalizes each captured segment after recording stops."
                        } else {
                            "This model uses offline segment decoding."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeechModelPicker(
    models: List<SpeechModelOption>,
    selectedId: ModelId?,
    enabled: Boolean,
    onSelect: (ModelId) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = models.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Speech model", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled && models.isNotEmpty()) {
            Text(selected?.displayName ?: "Install a speech model in Models")
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        when (operation) {
            SttOperation.RECORDING -> Button(onClick = onStop) { Text("Stop recording") }
            SttOperation.STOPPING -> OutlinedButton(onClick = {}, enabled = false) { Text("Stopping…") }
            SttOperation.IMPORTING -> {
                OutlinedButton(onClick = {}, enabled = false) { Text("Importing…") }
                TextButton(onClick = onCancel) { Text("Cancel import") }
            }
            SttOperation.TRANSCRIBING -> {
                OutlinedButton(onClick = {}, enabled = false) { Text("Transcribing…") }
                TextButton(onClick = onCancel) { Text("Cancel transcription") }
            }
            SttOperation.CANCELLING -> OutlinedButton(onClick = {}, enabled = false) { Text("Cancelling…") }
            SttOperation.IDLE -> {
                Button(onClick = onStart) { Text("Record") }
                OutlinedButton(onClick = onImport) { Text("Import audio") }
                if (hasInput) OutlinedButton(onClick = onRepeat) { Text("Repeat") }
                if (hasInput) TextButton(onClick = onClear) { Text("Clear") }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
