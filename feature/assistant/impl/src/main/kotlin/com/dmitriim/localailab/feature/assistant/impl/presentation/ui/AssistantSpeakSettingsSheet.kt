package com.dmitriim.localailab.feature.assistant.impl.presentation.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.OptionPickerItem
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.TtsModelOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.normalizeLanguageCode

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
        val selection = speechModelSelection(model, draft)
        draftModelId = modelId
        draft = selection.settings
        draftVoiceId = selection.voiceId
        commit(modelId, selection.voiceId, selection.settings)
    }
    val selectedModel = models.firstOrNull { it.id == draftModelId }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (selectingSpeechModel) {
            SpeakModelSelection(
                models = models,
                selectedModelId = draftModelId,
                enabled = enabled,
                onOpenModels = onOpenModels,
                onSelect = {
                    selectSpeechModel(ModelId(it))
                    selectingSpeechModel = false
                },
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
                SpeakModelAndVoiceSettings(
                    models = models,
                    selectedModel = selectedModel,
                    selectedModelId = draftModelId,
                    selectedVoiceId = draftVoiceId,
                    draft = draft,
                    enabled = enabled,
                    onChooseModel = { selectingSpeechModel = true },
                    onLanguageChange = { language, voiceId ->
                        draftVoiceId = voiceId
                        val candidate = draft.copy(languageCode = language)
                        draft = candidate
                        commit(draftModelId, voiceId, candidate)
                    },
                    onVoiceChange = { voiceId ->
                        draftVoiceId = voiceId
                        commit(draftModelId, voiceId, draft)
                    },
                )
                SpeakPlaybackSettings(
                    draft = draft,
                    enabled = enabled,
                    onChange = { candidate ->
                        draft = candidate
                        commit(draftModelId, draftVoiceId, candidate)
                    },
                )
                SpeakSettingsActions(
                    error = error,
                    enabled = enabled,
                    onReset = {
                        val candidate = SpeechOutputSettings()
                        val voiceId = selectedModel
                            ?.compatibleVoices(candidate.languageCode)
                            ?.firstOrNull()
                            ?.id
                        draft = candidate
                        draftVoiceId = voiceId
                        commit(draftModelId, voiceId, candidate)
                    },
                    onPreview = {
                        val modelId = draftModelId
                        val voiceId = draftVoiceId
                        error = when {
                            modelId == null -> selectSpeechModelError
                            voiceId == null -> selectCompatibleVoiceError
                            else -> onPreview(modelId, voiceId, draft)
                        }
                    },
                )
            }
        }
    }
}

private data class SpeechModelSelection(
    val settings: SpeechOutputSettings,
    val voiceId: String?,
)

private fun speechModelSelection(
    model: TtsModelOption?,
    settings: SpeechOutputSettings,
): SpeechModelSelection {
    val language = assistantTtsLanguages.firstOrNull { candidate ->
        model?.languages?.isEmpty() == true ||
            model?.languages?.any {
                normalizeLanguageCode(it) == candidate.code
            } == true
    }?.code ?: "en"
    return SpeechModelSelection(
        settings = settings.copy(languageCode = language),
        voiceId = model?.compatibleVoices(language)?.firstOrNull()?.id,
    )
}

@Composable
private fun SpeakModelSelection(
    models: List<TtsModelOption>,
    selectedModelId: ModelId?,
    enabled: Boolean,
    onOpenModels: () -> Unit,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
) {
    AssistantInSheetModelSelection(
        title = stringResource(CoreUiR.string.ui_copy_35),
        description = stringResource(CoreUiR.string.ui_description_15),
        items = models.map { OptionPickerItem(it.id.value, it.displayName, it.languages.joinToString(), it.installed) },
        selectedId = selectedModelId?.value,
        enabled = enabled,
        onSelect = onSelect,
        onOpenModels = onOpenModels,
        onBack = onBack,
    )
}

@Composable
private fun SpeakSettingsActions(
    error: String?,
    enabled: Boolean,
    onReset: () -> Unit,
    onPreview: () -> Unit,
) {
    error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    Text(
        text = stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_22),
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onReset, enabled = enabled) {
            Text(stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_23))
        }
        TextButton(onClick = onPreview, enabled = enabled) {
            Text(stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_24))
        }
    }
}

@Composable
private fun SpeakModelAndVoiceSettings(
    models: List<TtsModelOption>,
    selectedModel: TtsModelOption?,
    selectedModelId: ModelId?,
    selectedVoiceId: String?,
    draft: SpeechOutputSettings,
    enabled: Boolean,
    onChooseModel: () -> Unit,
    onLanguageChange: (String, String?) -> Unit,
    onVoiceChange: (String) -> Unit,
) {
    AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_model))
    AssistantInSheetModelPicker(
        label = stringResource(CoreUiR.string.ui_copy_37),
        items = models.map { OptionPickerItem(it.id.value, it.displayName, it.languages.joinToString(), it.installed) },
        selectedId = selectedModelId?.value,
        enabled = enabled,
        onClick = onChooseModel,
    )
    AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_voice_language))
    Text(
        text = stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_21),
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        assistantTtsLanguages.forEach { language ->
            val supported = selectedModel?.languages?.isEmpty() == true ||
                selectedModel?.languages?.any {
                    normalizeLanguageCode(it) == language.code
                } == true
            FilterChip(
                selected = draft.languageCode == language.code,
                onClick = {
                    onLanguageChange(
                        language.code,
                        selectedModel?.compatibleVoices(language.code)?.firstOrNull()?.id,
                    )
                },
                enabled = enabled && supported,
                label = { Text(stringResource(language.labelRes)) },
            )
        }
    }
    VoicePicker(
        selectedModel?.compatibleVoices(draft.languageCode).orEmpty().map { it.id to it.displayName },
        selectedVoiceId,
        onVoiceChange,
        enabled,
    )
}

@Composable
private fun SpeakPlaybackSettings(
    draft: SpeechOutputSettings,
    enabled: Boolean,
    onChange: (SpeechOutputSettings) -> Unit,
) {
    AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_playback))
    OutputField(
        label = stringResource(CoreUiR.string.assistant_settings_speech_rate),
        value = draft.speed,
        enabled = enabled,
        onChange = { onChange(draft.copy(speed = it)) },
    )
    OutputField(
        label = stringResource(CoreUiR.string.assistant_settings_volume),
        value = draft.volume,
        enabled = enabled,
        onChange = { onChange(draft.copy(volume = it)) },
    )
    OutputField(
        label = stringResource(CoreUiR.string.assistant_settings_sentence_silence),
        value = draft.sentenceSilenceScale,
        enabled = enabled,
        onChange = { onChange(draft.copy(sentenceSilenceScale = it)) },
    )
    AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_performance))
    OutputField(
        label = stringResource(CoreUiR.string.assistant_settings_thread_count),
        value = draft.threadCount,
        enabled = enabled,
        onChange = { onChange(draft.copy(threadCount = it.filter(Char::isDigit))) },
    )
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
        Text(
            text = stringResource(CoreUiR.string.assistant_assistant_speak_settings_sheet_25),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        )
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
        value = value,
        onValueChange = onChange,
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
