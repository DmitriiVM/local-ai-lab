package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.dmitriim.localaiplayground.ai.api.llm.LlmGenerationOption
import com.dmitriim.localaiplayground.ai.api.llm.LlmLoadOption
import com.dmitriim.localaiplayground.core.model.engine.ComputePreference
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.feature.assistant.presentation.ChatModelOption
import com.dmitriim.localaiplayground.feature.assistant.presentation.ChatSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssistantChatSettingsSheet(
    models: List<ChatModelOption>,
    selectedModelId: ModelId?,
    settings: ChatSettings,
    enabled: Boolean,
    onApply: (ModelId, ChatSettings) -> String?,
    onUnload: () -> Unit,
    onOpenModels: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draftModelId by remember(selectedModelId) { mutableStateOf(selectedModelId) }
    var draft by remember(settings) { mutableStateOf(settings) }
    var error by remember { mutableStateOf<String?>(null) }
    val commit = { modelId: ModelId?, candidate: ChatSettings ->
        error = if (modelId == null) "Select an installed chat model." else onApply(modelId, candidate)
    }
    val selectedModel = models.firstOrNull { it.id == draftModelId }
    val capabilities = selectedModel?.capabilities
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Chat settings", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            AssistantSettingsModelPicker(
                label = "Chat model",
                items = models.map { SettingsModelItem(it.id.value, it.displayName, it.installed) },
                selectedId = draftModelId?.value,
                onSelect = { value ->
                    val modelId = ModelId(value)
                    val model = models.firstOrNull { it.id == modelId }
                    val candidate = draft.copy(
                        computePreference = model?.supportedComputePreference(draft.computePreference)
                            ?: draft.computePreference,
                        contextSize = model?.defaultContextSize?.toString() ?: draft.contextSize,
                    )
                    draftModelId = modelId
                    draft = candidate
                    commit(modelId, candidate)
                },
                onOpenModels = onOpenModels,
                enabled = enabled,
            )
            capabilities?.computePreferences?.takeIf { it.size > 1 }?.let { computePreferences ->
                AssistantSettingsModelPicker(
                    label = "Compute",
                    items = computePreferences.map { preference ->
                        SettingsModelItem(preference.name, preference.displayName(), installed = true)
                    },
                    selectedId = draft.computePreference.name,
                    onSelect = { value ->
                        draft = draft.copy(computePreference = ComputePreference.valueOf(value))
                            .also { candidate -> commit(draftModelId, candidate) }
                    },
                    onOpenModels = {},
                    enabled = enabled,
                )
            }
            if (capabilities?.systemInstructions == true) {
                SettingField("System prompt", draft.systemPrompt, enabled) {
                    draft = draft.copy(systemPrompt = it).also { candidate -> commit(draftModelId, candidate) }
                }
            }
            if (capabilities?.generationOptions?.contains(LlmGenerationOption.TEMPERATURE) == true) {
                SettingField("Temperature (0–2)", draft.temperature, enabled) {
                    draft = draft.copy(temperature = it).also { candidate -> commit(draftModelId, candidate) }
                }
            }
            if (capabilities?.generationOptions?.contains(LlmGenerationOption.MAX_OUTPUT_TOKENS) == true) {
                SettingField("Maximum output tokens", draft.maxOutputTokens, enabled) {
                    draft = draft.copy(maxOutputTokens = it).also { candidate -> commit(draftModelId, candidate) }
                }
            }
            if (capabilities?.generationOptions?.contains(LlmGenerationOption.TOP_K) == true) {
                SettingField("Top-K (1–200)", draft.topK, enabled) {
                    draft = draft.copy(topK = it).also { candidate -> commit(draftModelId, candidate) }
                }
            }
            if (capabilities?.generationOptions?.contains(LlmGenerationOption.TOP_P) == true) {
                SettingField("Top-P (0.05–1)", draft.topP, enabled) {
                    draft = draft.copy(topP = it).also { candidate -> commit(draftModelId, candidate) }
                }
            }
            if (capabilities?.loadOptions?.contains(LlmLoadOption.CONTEXT_SIZE) == true) {
                SettingField("Context size", draft.contextSize, enabled) {
                    draft = draft.copy(contextSize = it).also { candidate -> commit(draftModelId, candidate) }
                }
            }
            if (capabilities?.generationOptions?.contains(LlmGenerationOption.SEED) == true) {
                SettingField("Seed (blank = engine-selected)", draft.seed, enabled) {
                    draft = draft.copy(seed = it).also { candidate -> commit(draftModelId, candidate) }
                }
            }
            if (capabilities?.loadOptions?.contains(LlmLoadOption.THREAD_COUNT) == true) {
                SettingField("Thread count (0 = default)", draft.threadCount, enabled) {
                    draft = draft.copy(threadCount = it).also { candidate -> commit(draftModelId, candidate) }
                }
            }
            error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            TextButton(onClick = onUnload, enabled = enabled) { Text("Unload model now") }
            TextButton(
                onClick = {
                    val model = models.firstOrNull { it.id == draftModelId }
                    val context = model?.defaultContextSize ?: 512
                    draft = ChatSettings(
                        computePreference = model?.supportedComputePreference(ComputePreference.CPU)
                            ?: ComputePreference.CPU,
                        contextSize = context.toString(),
                    ).also { candidate -> commit(draftModelId, candidate) }
                },
                enabled = enabled,
            ) { Text("Reset") }
        }
    }
}

private fun ComputePreference.displayName(): String = when (this) {
    ComputePreference.AUTO -> "Automatic"
    ComputePreference.CPU -> "CPU"
    ComputePreference.GPU -> "GPU"
    ComputePreference.NPU -> "NPU"
    ComputePreference.SYSTEM_SERVICE -> "System service"
}

@Composable
private fun SettingField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    )
}
