package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.ui.component.OptionPickerItem
import com.dmitriim.localaiplayground.feature.assistant.presentation.SpeechOutputSettings
import com.dmitriim.localaiplayground.feature.assistant.presentation.TtsModelOption
import androidx.compose.ui.res.stringResource
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssistantSpeakSettingsSheet(
    models: List<TtsModelOption>,
    selectedModelId: ModelId?,
    selectedVoiceId: String?,
    settings: SpeechOutputSettings,
    enabled: Boolean,
    onApply: (ModelId, String, SpeechOutputSettings) -> String?,
    onPreview: (ModelId, String, SpeechOutputSettings) -> String?,
    onOpenModels: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draftModelId by remember(selectedModelId) { mutableStateOf(selectedModelId) }
    var draftVoiceId by remember(selectedVoiceId) { mutableStateOf(selectedVoiceId) }
    var draft by remember(settings) { mutableStateOf(settings) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectingSpeechModel by remember { mutableStateOf(false) }
    val selectSpeechModelError = stringResource(CoreUiR.string.assistant_error_select_speech_model)
    val selectCompatibleVoiceError = stringResource(CoreUiR.string.assistant_error_select_compatible_voice)
    val commit = { modelId: ModelId?, voiceId: String?, candidate: SpeechOutputSettings ->
        error = when {
            modelId == null -> selectSpeechModelError
            voiceId == null -> selectCompatibleVoiceError
            else -> onApply(modelId, voiceId, candidate)
        }
    }
    val selectSpeechModel: (ModelId) -> Unit = { modelId ->
        val model = models.firstOrNull { it.id == modelId }
        val language = assistantTtsLanguages.firstOrNull { candidate ->
            model?.languages?.isEmpty() == true ||
                model?.languages?.any {
                    com.dmitriim.localaiplayground.feature.assistant.presentation.normalizeLanguageCode(it) == candidate.code
                } == true
        }?.code ?: "en"
        val candidate = draft.copy(languageCode = language)
        val voiceId = model?.compatibleVoices(language)?.firstOrNull()?.id
        draftModelId = modelId
        draft = candidate
        draftVoiceId = voiceId
        commit(modelId, voiceId, candidate)
    }
    val selectedModel = models.firstOrNull { it.id == draftModelId }
    val voices = selectedModel?.compatibleVoices(draft.languageCode).orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (selectingSpeechModel) {
            AssistantInSheetModelSelection(
                title = stringResource(CoreUiR.string.ui_copy_35),
                description = stringResource(CoreUiR.string.ui_description_15),
                items = models.map {
                    OptionPickerItem(
                        id = it.id.value,
                        label = it.displayName,
                        supportingText = it.languages.joinToString(),
                        installed = it.installed,
                    )
                },
                selectedId = draftModelId?.value,
                enabled = enabled,
                onSelect = {
                    selectSpeechModel(ModelId(it))
                    selectingSpeechModel = false
                },
                onOpenModels = onOpenModels,
                onBack = { selectingSpeechModel = false },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AssistantSettingsSheetHeader(
                    title = stringResource(CoreUiR.string.ui_copy_36),
                    description = stringResource(CoreUiR.string.ui_description_16),
                )
                AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_model))
                AssistantInSheetModelPicker(
                    label = stringResource(CoreUiR.string.ui_copy_37),
                    items = models.map {
                        OptionPickerItem(
                            id = it.id.value,
                            label = it.displayName,
                            supportingText = it.languages.joinToString(),
                            installed = it.installed,
                        )
                    },
                    selectedId = draftModelId?.value,
                    enabled = enabled,
                    onClick = { selectingSpeechModel = true },
                )
                AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_voice_language))
                Text(stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_21), style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    assistantTtsLanguages.forEach { language ->
                        val supported = selectedModel?.languages?.isEmpty() == true ||
                            selectedModel?.languages?.any {
                                com.dmitriim.localaiplayground.feature.assistant.presentation.normalizeLanguageCode(it) == language.code
                            } == true
                        FilterChip(
                            selected = draft.languageCode == language.code,
                            onClick = {
                                val candidate = draft.copy(languageCode = language.code)
                                val voiceId = selectedModel?.compatibleVoices(language.code)?.firstOrNull()?.id
                                draft = candidate
                                draftVoiceId = voiceId
                                commit(draftModelId, voiceId, candidate)
                            },
                            enabled = enabled && supported,
                            label = { Text(stringResource(language.labelRes)) },
                        )
                    }
                }
                VoicePicker(
                    voices = voices.map { it.id to it.displayName },
                    selectedVoiceId = draftVoiceId,
                    onSelect = { voiceId ->
                        draftVoiceId = voiceId
                        commit(draftModelId, voiceId, draft)
                    },
                    enabled = enabled,
                )
                AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_playback))
                OutputField(stringResource(CoreUiR.string.assistant_settings_speech_rate), draft.speed, enabled) {
                    draft = draft.copy(speed = it).also { candidate -> commit(draftModelId, draftVoiceId, candidate) }
                }
                OutputField(stringResource(CoreUiR.string.assistant_settings_volume), draft.volume, enabled) {
                    draft = draft.copy(volume = it).also { candidate -> commit(draftModelId, draftVoiceId, candidate) }
                }
                OutputField(stringResource(CoreUiR.string.assistant_settings_sentence_silence), draft.sentenceSilenceScale, enabled) {
                    draft = draft.copy(sentenceSilenceScale = it).also { candidate -> commit(draftModelId, draftVoiceId, candidate) }
                }
                AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_performance))
                OutputField(stringResource(CoreUiR.string.assistant_settings_thread_count), draft.threadCount, enabled) {
                    draft = draft.copy(threadCount = it.filter(Char::isDigit)).also { candidate -> commit(draftModelId, draftVoiceId, candidate) }
                }
                error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
                Text(stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_22),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            val candidate = SpeechOutputSettings()
                            val voiceId = selectedModel?.compatibleVoices(candidate.languageCode)?.firstOrNull()?.id
                            draft = candidate
                            draftVoiceId = voiceId
                            commit(draftModelId, voiceId, candidate)
                        },
                        enabled = enabled,
                    ) { Text(stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_23)) }
                    TextButton(
                        onClick = {
                            val modelId = draftModelId
                            val voiceId = draftVoiceId
                            error = when {
                                modelId == null -> selectSpeechModelError
                                voiceId == null -> selectCompatibleVoiceError
                                else -> onPreview(modelId, voiceId, draft)
                            }
                        },
                        enabled = enabled,
                    ) { Text(stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_24)) }
                }
            }
        }
    }
}

@Composable
private fun VoicePicker(
    voices: List<Pair<String, String>>,
    selectedVoiceId: String?,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = voices.firstOrNull { it.first == selectedVoiceId }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_25), style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && voices.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(selected?.second ?: stringResource(CoreUiR.string.assistant_no_compatible_voice)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            voices.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun OutputField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value,
        onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
    )
}

private data class AssistantTtsLanguage(val code: String, val labelRes: Int)

private val assistantTtsLanguages = listOf(
    AssistantTtsLanguage("en", CoreUiR.string.language_english),
    AssistantTtsLanguage("ru", CoreUiR.string.language_russian),
    AssistantTtsLanguage("zh", CoreUiR.string.language_chinese),
)
