package com.dmitriim.localailab.feature.assistant.impl.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.OptionPickerItem
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatModelOption

@Composable
internal fun AssistantChatModelPicker(
    models: List<ChatModelOption>,
    selectedId: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AssistantInSheetModelPicker(
        label = stringResource(CoreUiR.string.ui_copy_3),
        items = models.map { model ->
            model.toPickerItem(stringResource(CoreUiR.string.ui_copy_5))
        },
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
        title = stringResource(CoreUiR.string.ui_copy_4),
        description = stringResource(CoreUiR.string.ui_description_1),
        items = models.map { model ->
            model.toPickerItem(stringResource(CoreUiR.string.ui_copy_5))
        },
        selectedId = selectedId?.value,
        enabled = enabled,
        onSelect = { id -> models.firstOrNull { it.id.value == id }?.let { onSelect(it.id) } },
        onOpenModels = onOpenModels,
        onBack = onBack,
    )
}

private fun ChatModelOption.toPickerItem(supportingText: String) = OptionPickerItem(
    id = id.value,
    label = displayName,
    supportingText = supportingText,
    installed = installed,
)
