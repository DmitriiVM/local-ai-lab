package com.dmitriim.localailab.feature.assistant.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.InstalledOptionList
import com.dmitriim.localailab.core.ui.component.OptionPickerItem

@Composable
internal fun AssistantInSheetModelPicker(
    label: String,
    items: List<OptionPickerItem>,
    selectedId: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val selected = items.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = onClick,
            enabled = enabled && items.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selected?.label ?: stringResource(CoreUiR.string.assistant_install_model),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun AssistantInSheetModelSelection(
    title: String,
    description: String,
    items: List<OptionPickerItem>,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onOpenModels: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text(stringResource(CoreUiR.string.assistant_assistant_in_sheet_model_picker_9)) }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 12.dp),
        )
        InstalledOptionList(
            items = items,
            selectedId = selectedId,
            enabled = enabled,
            onSelect = onSelect,
            onOpenAvailableItems = onOpenModels,
        )
    }
}
