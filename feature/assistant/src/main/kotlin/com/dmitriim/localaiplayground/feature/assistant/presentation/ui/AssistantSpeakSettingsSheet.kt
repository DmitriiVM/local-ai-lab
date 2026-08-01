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
import com.dmitriim.localaiplayground.feature.assistant.presentation.SpeechOutputSettings
import com.dmitriim.localaiplayground.feature.assistant.presentation.TtsModelOption

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
    val commit = { modelId: ModelId?, voiceId: String?, candidate: SpeechOutputSettings ->
        error = when {
            modelId == null -> "Select an installed speech model."
            voiceId == null -> "Select a compatible voice."
            else -> onApply(modelId, voiceId, candidate)
        }
    }
    val selectedModel = models.firstOrNull { it.id == draftModelId }
    val voices = selectedModel?.compatibleVoices(draft.languageCode).orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Text-to-speech settings", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            AssistantSettingsModelPicker(
                label = "Speech model",
                items = models.map { SettingsModelItem(it.id.value, it.displayName, it.installed) },
                selectedId = draftModelId?.value,
                onSelect = { value ->
                    val modelId = ModelId(value)
                    val model = models.firstOrNull { it.id == modelId }
                    val language = assistantTtsLanguages.firstOrNull { candidate ->
                        model?.languages?.isEmpty() == true || model?.languages?.any {
                            com.dmitriim.localaiplayground.feature.assistant.presentation.normalizeLanguageCode(it) == candidate.code
                        } == true
                    }?.code ?: "en"
                    val candidate = draft.copy(languageCode = language)
                    val voiceId = model?.compatibleVoices(language)?.firstOrNull()?.id
                    draftModelId = modelId
                    draft = candidate
                    draftVoiceId = voiceId
                    commit(modelId, voiceId, candidate)
                },
                onOpenModels = onOpenModels,
                enabled = enabled,
            )
            Text("Speech language", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                assistantTtsLanguages.forEach { language ->
                    val supported = selectedModel?.languages?.isEmpty() == true || selectedModel?.languages?.any {
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
                        label = { Text(language.label) },
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
            OutputField("Speech rate (0.5–2)", draft.speed, enabled) {
                draft = draft.copy(speed = it).also { candidate -> commit(draftModelId, draftVoiceId, candidate) }
            }
            OutputField("Volume (0–1)", draft.volume, enabled) {
                draft = draft.copy(volume = it).also { candidate -> commit(draftModelId, draftVoiceId, candidate) }
            }
            OutputField("Sentence silence (0–2)", draft.sentenceSilenceScale, enabled) {
                draft = draft.copy(sentenceSilenceScale = it).also { candidate -> commit(draftModelId, draftVoiceId, candidate) }
            }
            OutputField("Thread count (0 = default)", draft.threadCount, enabled) {
                draft = draft.copy(threadCount = it.filter(Char::isDigit)).also { candidate -> commit(draftModelId, draftVoiceId, candidate) }
            }
            error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            Text(
                "Reference voices are created and managed in the Text to Speech playground.",
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
                ) { Text("Reset") }
                TextButton(
                    onClick = {
                        val modelId = draftModelId
                        val voiceId = draftVoiceId
                        error = when {
                            modelId == null -> "Select an installed speech model."
                            voiceId == null -> "Select a compatible voice."
                            else -> onPreview(modelId, voiceId, draft)
                        }
                    },
                    enabled = enabled,
                ) { Text("Preview") }
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
        Text("Voice", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && voices.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(selected?.second ?: "No compatible voice") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            voices.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(id); expanded = false },
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

private data class AssistantTtsLanguage(val code: String, val label: String)

private val assistantTtsLanguages = listOf(
    AssistantTtsLanguage("en", "English"),
    AssistantTtsLanguage("ru", "Russian"),
    AssistantTtsLanguage("zh", "Chinese"),
)
