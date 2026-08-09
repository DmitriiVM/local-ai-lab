package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.runtime.Composable
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.ui.component.OptionPickerItem
import com.dmitriim.localaiplayground.feature.assistant.presentation.ChatModelOption

@Composable
internal fun AssistantChatModelPicker(
    models: List<ChatModelOption>,
    selectedId: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AssistantInSheetModelPicker(
        label = "Chat model",
        items = models.map(ChatModelOption::toPickerItem),
        selectedId = selectedId,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
internal fun AssistantChatModelSelection(
    models: List<ChatModelOption>,
    selectedId: ModelId?,
    enabled: Boolean,
    onSelect: (ModelId) -> Unit,
    onOpenModels: () -> Unit,
    onBack: () -> Unit,
) {
    AssistantInSheetModelSelection(
        title = "Chat model",
        description = "Choose an installed model for private local chat.",
        items = models.map(ChatModelOption::toPickerItem),
        selectedId = selectedId?.value,
        enabled = enabled,
        onSelect = { id -> models.firstOrNull { it.id.value == id }?.let { onSelect(it.id) } },
        onOpenModels = onOpenModels,
        onBack = onBack,
    )
}

private fun ChatModelOption.toPickerItem() = OptionPickerItem(
    id = id.value,
    label = displayName,
    supportingText = "",
    installed = installed,
)
