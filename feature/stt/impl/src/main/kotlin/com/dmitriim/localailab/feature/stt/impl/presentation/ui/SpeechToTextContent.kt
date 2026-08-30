package com.dmitriim.localailab.feature.stt.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.AppSectionCard
import com.dmitriim.localailab.core.ui.component.AppSurfaceTone
import com.dmitriim.localailab.core.ui.component.StatusMessage
import com.dmitriim.localailab.core.ui.style.AppFilterChipDefaults
import com.dmitriim.localailab.core.ui.text.asString
import com.dmitriim.localailab.feature.stt.impl.presentation.SpeechToTextUiState
import com.dmitriim.localailab.feature.stt.impl.presentation.SttLanguage
import com.dmitriim.localailab.feature.stt.impl.presentation.SttOperation

@Composable
internal fun SpeechToTextSetup(
    state: SpeechToTextUiState,
    busy: Boolean,
    onSelectModel: (ModelId) -> Unit,
    onSelectLanguage: (SttLanguage) -> Unit,
    onThreadCountChange: (String) -> Unit,
) {
    AppSectionCard("Setup", tone = AppSurfaceTone.TONAL) {
        SpeechModelPicker(
            models = state.models,
            selectedId = state.selectedModelId,
            enabled = !busy,
            onSelect = onSelectModel,
        )
        Text(
            text = stringResource(CoreUiR.string.stt_speech_to_text_screen_133),
            style = MaterialTheme.typography.titleSmall,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.availableLanguages.size) { index ->
                val language = state.availableLanguages[index]
                FilterChip(
                    selected = state.language == language,
                    onClick = { onSelectLanguage(language) },
                    enabled = !busy,
                    label = { Text(language.label) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
        OutlinedTextField(
            value = state.threadCount,
            onValueChange = onThreadCountChange,
            enabled = !busy,
            label = { Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_134)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 64.dp),
        )
    }
}

@Composable
internal fun SpeechToTextAudioInput(
    state: SpeechToTextUiState,
    busy: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onImport: () -> Unit,
    onRepeat: () -> Unit,
    onProfile: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    AppSectionCard("Audio input", tone = AppSurfaceTone.TONAL) {
        RecordingControls(
            operation = state.operation,
            hasInput = state.input != null,
            onStart = onStart,
            onStop = onStop,
            onImport = onImport,
            onRepeat = onRepeat,
            onCancel = onCancel,
            onClear = onClear,
        )
        state.level?.let { level ->
            Text(
                text = stringResource(
                    CoreUiR.string.stt_speech_to_text_screen_format_13,
                    formatDuration(level.elapsedMs),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    CoreUiR.string.stt_speech_to_text_screen_format_14,
                    (level.rms * 100).toInt(),
                    (level.peak * 100).toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        state.input?.let { input ->
            Text(
                text = stringResource(
                    CoreUiR.string.stt_speech_to_text_screen_format_15,
                    input.displayName,
                    formatDuration(input.durationMs),
                    input.sourceDescription,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onProfile,
            enabled = !busy && state.input != null && state.selectedModel?.installed == true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(CoreUiR.string.stt_speech_to_text_screen_135))
        }
    }
}

@Composable
internal fun SpeechToTextStatus(state: SpeechToTextUiState) {
    state.errorMessage?.let { errorMessage ->
        StatusMessage(
            title = stringResource(CoreUiR.string.ui_copy_68),
            explanation = errorMessage.asString(),
        )
    }
    if (state.operation != SttOperation.TRANSCRIBING) return
    val androidRecognizer = state.selectedModel?.engineId?.value == "android-speech-recognizer"
    StatusMessage(
        title = stringResource(
            if (androidRecognizer) {
                CoreUiR.string.stt_processing_recording
            } else {
                CoreUiR.string.stt_transcribing_locally
            },
        ),
        explanation = if (androidRecognizer) {
            "Recording has stopped. Android SpeechRecognizer is receiving the captured " +
                "audio in real time, so this takes about as long as the recording."
        } else {
            "${state.selectedModel?.displayName.orEmpty()} is processing bounded " +
                "30-second audio segments. You can cancel at any time."
        },
    )
}
