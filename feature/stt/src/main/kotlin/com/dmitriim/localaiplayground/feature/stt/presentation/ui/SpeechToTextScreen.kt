package com.dmitriim.localaiplayground.feature.stt.presentation.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.core.ui.style.AppSurfaceStyle
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
            .padding(start = 16.dp, top = dimensions.topBarOverlayClearance + 44.dp, end = 16.dp, bottom = 24.dp + dimensions.bottomNavigationOverlayClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Local speech to text", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Choose the Android on-device recognizer or an installed speech model. Recording shows live levels, then final text appears after you stop.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SttSectionCard("Setup") {
            SpeechModelPicker(state.models, state.selectedModelId, enabled = !busy, onSelectModel)
            Text("Language", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.availableLanguages.size) { index ->
                    val language = state.availableLanguages[index]
                    FilterChip(
                        selected = state.language == language,
                        onClick = { onSelectLanguage(language) },
                        enabled = !busy,
                        label = { Text(language.label) },
                        colors = sttFilterChipColors(),
                    )
                }
            }
            OutlinedTextField(
                value = state.threadCount,
                onValueChange = onThreadCountChange,
                enabled = !busy,
                label = { Text("CPU threads (0 = safe default)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp, max = 64.dp),
            )
        }
        SttSectionCard("Audio input") {
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
                Text("Recording ${formatDuration(level.elapsedMs)}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Live input level: ${(level.rms * 100).toInt()}% RMS · ${(level.peak * 100).toInt()}% peak",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.input?.let { input ->
                Text(
                    "Input: ${input.displayName} · ${formatDuration(input.durationMs)} · ${input.sourceDescription}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onProfile,
                enabled = !busy && state.input != null && state.selectedModel?.installed == true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Profile current audio")
            }
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
            SttSectionCard("Final transcript") {
                    Text(
                        state.transcript,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.SansSerif,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
                    ) {
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
                        TextButton(
                            onClick = onClear,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("Clear") }
                    }
            }
        }
        state.metrics?.let { metrics ->
            SttRunMetricsCard(
                metrics = metrics,
                streamingModel = state.selectedModel?.recognitionMode ==
                    com.dmitriim.localaiplayground.core.model.manifest.SttRecognitionMode.STREAMING,
            )
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
                ) { Text("Record", maxLines = 1, softWrap = false) }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onImport,
                ) { Text("Import audio", maxLines = 1, softWrap = false) }
            }
            if (hasInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onRepeat,
                    ) { Text("Repeat", maxLines = 1, softWrap = false) }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onClear,
                    ) { Text("Clear", maxLines = 1, softWrap = false) }
                }
            }
        }
    }
}

@Composable
private fun SttSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = AppSurfaceStyle.cardBorderBrush(colors),
                shape = AppSurfaceStyle.CardShape,
            ),
        shape = AppSurfaceStyle.CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppSurfaceStyle.cardBackgroundBrush(colors))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun sttFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
    containerColor = Color.Transparent,
)

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
