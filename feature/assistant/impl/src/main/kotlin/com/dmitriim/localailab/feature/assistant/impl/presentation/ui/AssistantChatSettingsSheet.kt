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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.chat.LlmGenerationOption
import com.dmitriim.localailab.ai.api.chat.LlmLoadOption
import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatModelOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatSettings

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
    var selectingChatModel by remember { mutableStateOf(false) }
    var advancedSettingsVisible by remember { mutableStateOf(false) }
    val commit = { modelId: ModelId?, candidate: ChatSettings ->
        error = if (modelId == null) "Select an installed chat model." else onApply(modelId, candidate)
    }
    val selectChatModel: (ModelId) -> Unit = { modelId ->
        val model = models.firstOrNull { it.id == modelId }
        val candidate = draft.copy(
            computePreference = model?.supportedComputePreference(draft.computePreference)
                ?: draft.computePreference,
            contextSize = model?.defaultContextSize?.toString() ?: draft.contextSize,
        )
        draftModelId = modelId
        draft = candidate
        commit(modelId, candidate)
    }
    val selectedModel = models.firstOrNull { it.id == draftModelId }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (selectingChatModel) {
            AssistantChatModelSelection(
                models = models,
                selectedId = draftModelId,
                enabled = enabled,
                onSelect = {
                    selectChatModel(it)
                    selectingChatModel = false
                },
                onOpenModels = onOpenModels,
                onBack = { selectingChatModel = false },
            )
        } else {
            ChatSettingsEditor(
                models, selectedModel, draftModelId, draft, error, advancedSettingsVisible, enabled,
                onSelectModel = { selectingChatModel = true },
                onDraftChange = { candidate ->
                    draft = candidate
                    commit(draftModelId, candidate)
                },
                onAdvancedVisibilityChange = { advancedSettingsVisible = it },
                onUnload = onUnload,
                onReset = {
                    val context = selectedModel?.defaultContextSize ?: 512
                    ChatSettings(
                        computePreference = selectedModel?.supportedComputePreference(ComputePreference.CPU)
                            ?: ComputePreference.CPU,
                        contextSize = context.toString(),
                    )
                },
            )
        }
    }
}

@Composable
private fun ChatSettingsEditor(
    models: List<ChatModelOption>,
    selectedModel: ChatModelOption?,
    selectedModelId: ModelId?,
    draft: ChatSettings,
    error: String?,
    advancedVisible: Boolean,
    enabled: Boolean,
    onSelectModel: () -> Unit,
    onDraftChange: (ChatSettings) -> Unit,
    onAdvancedVisibilityChange: (Boolean) -> Unit,
    onUnload: () -> Unit,
    onReset: () -> ChatSettings,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AssistantSettingsSheetHeader(
            title = stringResource(CoreUiR.string.ui_copy_6),
            description = stringResource(CoreUiR.string.ui_description_2),
        )
        ChatModelSettings(
            models = models,
            model = selectedModel,
            selectedModelId = selectedModelId,
            draft = draft,
            enabled = enabled,
            onSelectModel = onSelectModel,
            onDraftChange = onDraftChange,
        )
        ChatGenerationSettings(selectedModel, draft, enabled, onDraftChange)
        ChatAdvancedSettings(
            model = selectedModel,
            draft = draft,
            visible = advancedVisible,
            enabled = enabled,
            onVisibilityChange = onAdvancedVisibilityChange,
            onDraftChange = onDraftChange,
        )
        error?.let {
            Text(
                text = it,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
            )
        }
        ChatSettingsActions(enabled, onUnload) { onDraftChange(onReset()) }
    }
}

@Composable
private fun ChatModelSettings(
    models: List<ChatModelOption>,
    model: ChatModelOption?,
    selectedModelId: ModelId?,
    draft: ChatSettings,
    enabled: Boolean,
    onSelectModel: () -> Unit,
    onDraftChange: (ChatSettings) -> Unit,
) {
    AssistantSettingsSection("Model")
    AssistantChatModelPicker(
        models = models,
        selectedId = selectedModelId?.value,
        enabled = enabled,
        onClick = onSelectModel,
    )
    model?.capabilities?.computePreferences?.takeIf { it.size > 1 }?.let { preferences ->
        AssistantSettingsModelPicker(
            label = stringResource(CoreUiR.string.ui_copy_7),
            items = preferences.map {
                SettingsModelItem(
                    id = it.name,
                    name = it.displayName(),
                    installed = true,
                )
            },
            selectedId = draft.computePreference.name,
            onSelect = { onDraftChange(draft.copy(computePreference = ComputePreference.valueOf(it))) },
            onOpenModels = {},
            enabled = enabled,
        )
    }
}

@Composable
private fun ChatGenerationSettings(
    model: ChatModelOption?,
    draft: ChatSettings,
    enabled: Boolean,
    onDraftChange: (ChatSettings) -> Unit,
) {
    val capabilities = model?.capabilities ?: return
    if (capabilities.systemInstructions) {
        AssistantSettingsSection("Instructions")
        SettingField(
            label = "System prompt",
            value = draft.systemPrompt,
            enabled = enabled,
            onChange = { onDraftChange(draft.copy(systemPrompt = it)) },
        )
    }
    val options = capabilities.generationOptions
    if (options.any { it in chatGenerationOptions }) AssistantSettingsSection("Generation")
    if (LlmGenerationOption.TEMPERATURE in options) {
        SettingField(
            label = "Temperature (0–2)",
            value = draft.temperature,
            enabled = enabled,
            onChange = { onDraftChange(draft.copy(temperature = it)) },
        )
    }
    if (LlmGenerationOption.MAX_OUTPUT_TOKENS in options) {
        SettingField(
            label = "Maximum output tokens",
            value = draft.maxOutputTokens,
            enabled = enabled,
            onChange = { onDraftChange(draft.copy(maxOutputTokens = it)) },
        )
    }
    if (LlmGenerationOption.TOP_K in options) {
        SettingField(
            label = "Top-K (1–200)",
            value = draft.topK,
            enabled = enabled,
            onChange = { onDraftChange(draft.copy(topK = it)) },
        )
    }
    if (LlmGenerationOption.TOP_P in options) {
        SettingField(
            label = "Top-P (0.05–1)",
            value = draft.topP,
            enabled = enabled,
            onChange = { onDraftChange(draft.copy(topP = it)) },
        )
    }
    if (LlmLoadOption.CONTEXT_SIZE in capabilities.loadOptions) {
        SettingField(
            label = "Context size",
            value = draft.contextSize,
            enabled = enabled,
            onChange = { onDraftChange(draft.copy(contextSize = it)) },
        )
    }
}

@Composable
private fun ChatAdvancedSettings(
    model: ChatModelOption?,
    draft: ChatSettings,
    visible: Boolean,
    enabled: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    onDraftChange: (ChatSettings) -> Unit,
) {
    val capabilities = model?.capabilities ?: return
    val canSetSeed = LlmGenerationOption.SEED in capabilities.generationOptions
    val canSetThreads = LlmLoadOption.THREAD_COUNT in capabilities.loadOptions
    if (!canSetSeed && !canSetThreads) return
    TextButton(onClick = { onVisibilityChange(!visible) }, enabled = enabled) {
        Text(
            stringResource(
                if (visible) {
                    CoreUiR.string.assistant_hide_advanced_settings
                } else {
                    CoreUiR.string.assistant_show_advanced_settings
                },
            ),
        )
    }
    if (!visible) return
    AssistantSettingsSection("Advanced")
    if (canSetSeed) {
        SettingField(
            label = "Seed (blank = engine-selected)",
            value = draft.seed,
            enabled = enabled,
            onChange = { onDraftChange(draft.copy(seed = it)) },
        )
    }
    if (canSetThreads) {
        SettingField(
            label = "Thread count (0 = default)",
            value = draft.threadCount,
            enabled = enabled,
            onChange = { onDraftChange(draft.copy(threadCount = it)) },
        )
    }
}

@Composable
private fun ChatSettingsActions(enabled: Boolean, onUnload: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onUnload, enabled = enabled) {
            Text(stringResource(CoreUiR.string.assistant_assistant_chat_settings_sheet_2))
        }
        TextButton(onClick = onReset, enabled = enabled) {
            Text(stringResource(CoreUiR.string.assistant_assistant_chat_settings_sheet_3))
        }
    }
}

private val chatGenerationOptions = setOf(
    LlmGenerationOption.TEMPERATURE,
    LlmGenerationOption.MAX_OUTPUT_TOKENS,
    LlmGenerationOption.TOP_K,
    LlmGenerationOption.TOP_P,
)

@Composable
private fun ComputePreference.displayName(): String = when (this) {
    ComputePreference.AUTO -> stringResource(CoreUiR.string.compute_automatic)
    ComputePreference.CPU -> stringResource(CoreUiR.string.compute_cpu)
    ComputePreference.GPU -> stringResource(CoreUiR.string.compute_gpu)
    ComputePreference.NPU -> stringResource(CoreUiR.string.compute_npu)
    ComputePreference.SYSTEM_SERVICE -> stringResource(CoreUiR.string.compute_system_service)
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
