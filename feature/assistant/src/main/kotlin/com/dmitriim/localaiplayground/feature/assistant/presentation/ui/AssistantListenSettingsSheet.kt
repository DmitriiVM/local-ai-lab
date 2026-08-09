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
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.feature.assistant.presentation.SpeechInputSettings
import com.dmitriim.localaiplayground.feature.assistant.presentation.SpeechModelOption
import com.dmitriim.localaiplayground.feature.assistant.presentation.normalizeLanguageCode

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
                title = "Recognition model",
                description = "Choose an installed model for on-device speech recognition.",
                items = models.map {
                    AssistantModelSelectionItem(
                        id = it.id.value,
                        name = it.displayName,
                        detail = it.languages.joinToString(),
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
        } else Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AssistantSettingsSheetHeader(
                title = "Speech-to-text settings",
                description = "Choose a recognizer and language for voice input.",
            )
            AssistantSettingsSection("Model")
            AssistantInSheetModelPicker(
                label = "Recognition model",
                items = models.map {
                    AssistantModelSelectionItem(
                        id = it.id.value,
                        name = it.displayName,
                        detail = it.languages.joinToString(),
                        installed = it.installed,
                    )
                },
                selectedId = draftModelId?.value,
                enabled = enabled,
                onClick = { selectingRecognitionModel = true },
            )
            AssistantSettingsSection("Recognition")
            Text("Recognition language", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.take(3).forEach { language ->
                    FilterChip(
                        selected = draft.languageCode == language.code,
                        onClick = {
                            draft = draft.copy(languageCode = language.code).also { candidate -> commit(draftModelId, candidate) }
                        },
                        enabled = enabled,
                        label = { Text(language.label) },
                    )
                }
            }
            if (languages.size > 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.drop(3).forEach { language ->
                        FilterChip(
                            selected = draft.languageCode == language.code,
                            onClick = {
                                draft = draft.copy(languageCode = language.code).also { candidate -> commit(draftModelId, candidate) }
                            },
                            enabled = enabled,
                            label = { Text(language.label) },
                        )
                    }
                }
            }
            AssistantSettingsSection("Performance")
            OutlinedTextField(
                value = draft.threadCount,
                onValueChange = {
                    draft = draft.copy(threadCount = it.filter(Char::isDigit)).also { candidate -> commit(draftModelId, candidate) }
                },
                label = { Text("Thread count (0 = default)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled,
            )
            error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { draft = SpeechInputSettings().also { candidate -> commit(draftModelId, candidate) } },
                    enabled = enabled,
                ) { Text("Reset") }
            }
        }
    }
}

private data class AssistantLanguage(val code: String, val label: String)

private val assistantSttLanguages = listOf(
    AssistantLanguage("en", "English"),
    AssistantLanguage("ru", "Russian"),
    AssistantLanguage("zh", "Chinese"),
    AssistantLanguage("ja", "Japanese"),
    AssistantLanguage("ko", "Korean"),
    AssistantLanguage("yue", "Cantonese"),
)
