package com.dmitriim.localailab.feature.assistant.impl.presentation.ui.stt

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
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
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechInputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechModelOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.normalizeLanguageCode
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.AssistantInSheetModelPicker
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.AssistantInSheetModelSelection
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.AssistantSettingsSection
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.AssistantSettingsSheetHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssistantListenSettingsSheet(
    models: List<SpeechModelOption>,
    selectedModelId: ModelId?,
    settings: SpeechInputSettings,
    enabled: Boolean,
    onApply: (ModelId, SpeechInputSettings) -> String?,
    onOpenModels: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draftModelId by remember(selectedModelId) { mutableStateOf(selectedModelId) }
    var draft by remember(settings) { mutableStateOf(settings) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectingRecognitionModel by remember { mutableStateOf(false) }
    val commit = { modelId: ModelId?, candidate: SpeechInputSettings ->
        error = if (modelId == null) "Select an installed recognition model." else onApply(modelId, candidate)
    }
    val selectRecognitionModel: (ModelId) -> Unit = { modelId ->
        val model = models.firstOrNull { it.id == modelId }
        val candidate = if (model != null && !model.supports(draft.languageCode)) {
            draft.copy(
                languageCode = model.languages.firstOrNull()?.let(::normalizeLanguageCode) ?: "en",
            )
        } else {
            draft
        }
        draftModelId = modelId
        draft = candidate
        commit(modelId, candidate)
    }
    val selectedModel = models.firstOrNull { it.id == draftModelId }
    val languages = assistantSttLanguages.filter { selectedModel?.supports(it.code) != false }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (selectingRecognitionModel) {
            AssistantInSheetModelSelection(
                title = stringResource(CoreUiR.string.ui_copy_21),
                description = stringResource(CoreUiR.string.ui_description_3),
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
                    selectRecognitionModel(ModelId(it))
                    selectingRecognitionModel = false
                },
                onOpenModels = onOpenModels,
                onBack = { selectingRecognitionModel = false },
            )
        } else {
            ListenSettingsContent(
                models = models,
                draftModelId = draftModelId,
                draft = draft,
                languages = languages,
                enabled = enabled,
                error = error,
                onChooseModel = { selectingRecognitionModel = true },
                onDraftChange = { candidate ->
                    draft = candidate
                    commit(draftModelId, candidate)
                },
            )
        }
    }
}

@Composable
private fun ListenSettingsContent(
    models: List<SpeechModelOption>,
    draftModelId: ModelId?,
    draft: SpeechInputSettings,
    languages: List<AssistantLanguage>,
    enabled: Boolean,
    error: String?,
    onChooseModel: () -> Unit,
    onDraftChange: (SpeechInputSettings) -> Unit,
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
            title = stringResource(CoreUiR.string.ui_copy_22),
            description = stringResource(CoreUiR.string.ui_description_4),
        )
        AssistantSettingsSection("Model")
        AssistantInSheetModelPicker(
            label = stringResource(CoreUiR.string.ui_copy_23),
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
            onClick = onChooseModel,
        )
        ListenLanguageSettings(languages, draft, enabled, onDraftChange)
        ListenPerformanceSettings(draft, enabled, error, onDraftChange)
    }
}

@Composable
private fun ListenLanguageSettings(
    languages: List<AssistantLanguage>,
    draft: SpeechInputSettings,
    enabled: Boolean,
    onDraftChange: (SpeechInputSettings) -> Unit,
) {
    AssistantSettingsSection("Recognition")
    Text(
        text = stringResource(CoreUiR.string.assistant_assistant_listen_settings_sheet_12),
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
    )
    languages.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { language ->
                FilterChip(
                    selected = draft.languageCode == language.code,
                    onClick = { onDraftChange(draft.copy(languageCode = language.code)) },
                    enabled = enabled,
                    label = { Text(stringResource(language.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun ListenPerformanceSettings(
    draft: SpeechInputSettings,
    enabled: Boolean,
    error: String?,
    onDraftChange: (SpeechInputSettings) -> Unit,
) {
    AssistantSettingsSection("Performance")
    OutlinedTextField(
        value = draft.threadCount,
        onValueChange = { onDraftChange(draft.copy(threadCount = it.filter(Char::isDigit))) },
        label = {
            Text(stringResource(CoreUiR.string.assistant_assistant_listen_settings_sheet_13))
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
    )
    error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = { onDraftChange(SpeechInputSettings()) },
            enabled = enabled,
        ) {
            Text(stringResource(CoreUiR.string.assistant_assistant_listen_settings_sheet_14))
        }
    }
}

private data class AssistantLanguage(val code: String, val labelRes: Int)

private val assistantSttLanguages = listOf(
    AssistantLanguage("en", CoreUiR.string.language_english),
    AssistantLanguage("ru", CoreUiR.string.language_russian),
    AssistantLanguage("zh", CoreUiR.string.language_chinese),
    AssistantLanguage("ja", CoreUiR.string.language_japanese),
    AssistantLanguage("ko", CoreUiR.string.language_korean),
    AssistantLanguage("yue", CoreUiR.string.language_cantonese),
)
