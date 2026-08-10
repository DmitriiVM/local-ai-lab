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
