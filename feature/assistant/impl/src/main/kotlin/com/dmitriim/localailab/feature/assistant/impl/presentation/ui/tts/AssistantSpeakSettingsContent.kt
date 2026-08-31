package com.dmitriim.localailab.feature.assistant.impl.presentation.ui.tts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.OptionPickerItem
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.TtsModelOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.normalizeLanguageCode
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.AssistantInSheetModelPicker
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.AssistantInSheetModelSelection
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.AssistantSettingsSection
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.AssistantSettingsSheetHeader

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SpeakSettingsSheetBody(
    selectingSpeechModel: Boolean,
    models: List<TtsModelOption>,
    selectedModel: TtsModelOption?,
    draftModelId: ModelId?,
    draftVoiceId: String?,
    draft: SpeechOutputSettings,
    error: String?,
    enabled: Boolean,
    onOpenModels: () -> Unit,
    onSelectSpeechModel: (ModelId) -> Unit,
    onBackToSettings: () -> Unit,
    onChooseModel: () -> Unit,
    onLanguageChange: (String, String?) -> Unit,
    onVoiceChange: (String?) -> Unit,
    onChange: (SpeechOutputSettings) -> Unit,
    onReset: () -> Unit,
    onPreview: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (selectingSpeechModel) {
            SpeakModelSelection(
                models = models,
                selectedModelId = draftModelId,
                enabled = enabled,
                onOpenModels = onOpenModels,
                onSelect = { onSelectSpeechModel(ModelId(it)) },
                onBack = onBackToSettings,
            )
        } else {
            SpeakSettingsContent(
                models = models,
                selectedModel = selectedModel,
                selectedModelId = draftModelId,
                selectedVoiceId = draftVoiceId,
                draft = draft,
                error = error,
                enabled = enabled,
                onChooseModel = onChooseModel,
                onLanguageChange = onLanguageChange,
                onVoiceChange = onVoiceChange,
                onChange = onChange,
                onReset = onReset,
                onPreview = onPreview,
            )
        }
    }
}

@Composable
private fun SpeakSettingsContent(
    models: List<TtsModelOption>,
    selectedModel: TtsModelOption?,
    selectedModelId: ModelId?,
    selectedVoiceId: String?,
    draft: SpeechOutputSettings,
    error: String?,
    enabled: Boolean,
    onChooseModel: () -> Unit,
    onLanguageChange: (String, String?) -> Unit,
    onVoiceChange: (String?) -> Unit,
    onChange: (SpeechOutputSettings) -> Unit,
    onReset: () -> Unit,
    onPreview: () -> Unit,
) {
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
            selectedModelId = selectedModelId,
            selectedVoiceId = selectedVoiceId,
            draft = draft,
            enabled = enabled,
            onChooseModel = onChooseModel,
            onLanguageChange = onLanguageChange,
            onVoiceChange = onVoiceChange,
        )
        SpeakPlaybackSettings(draft = draft, enabled = enabled, onChange = onChange)
        SpeakSettingsActions(
            error = error,
            enabled = enabled,
            onReset = onReset,
            onPreview = onPreview,
        )
    }
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
    Column {
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
    onVoiceChange: (String?) -> Unit,
) {
    Column {
        AssistantSettingsSection(stringResource(CoreUiR.string.assistant_settings_model))
        AssistantInSheetModelPicker(
            label = stringResource(CoreUiR.string.ui_copy_37),
            items = models.map {
                OptionPickerItem(it.id.value, it.displayName, it.languages.joinToString(), it.installed)
            },
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
                    selectedModel?.languages?.any { normalizeLanguageCode(it) == language.code } == true
                androidx.compose.material3.FilterChip(
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
            voices = selectedModel?.compatibleVoices(draft.languageCode).orEmpty().map {
                it.id to it.displayName
            },
            selectedVoiceId = selectedVoiceId,
            onSelect = { onVoiceChange(it) },
            enabled = enabled,
        )
    }
}
