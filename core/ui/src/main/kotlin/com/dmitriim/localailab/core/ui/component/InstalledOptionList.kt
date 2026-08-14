package com.dmitriim.localailab.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR

@Composable
fun InstalledOptionList(
    items: List<OptionPickerItem>,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    noInstalledItemsLabel: String = "No compatible models are installed.",
    onOpenAvailableItems: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val installedItems = items.filter(OptionPickerItem::installed)
    val availableItems = items.filterNot(OptionPickerItem::installed)
    var showAvailableItems by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp),
    ) {
        item {
            Text(
                text = stringResource(CoreUiR.string.ui_copy_1),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        if (installedItems.isEmpty()) {
            item {
                Text(
                    text = noInstalledItemsLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        } else {
            items(installedItems, key = OptionPickerItem::id) { item ->
                InstalledOptionRow(
                    item = item,
                    selected = item.id == selectedId,
                    enabled = enabled,
                    onClick = { onSelect(item.id) },
                )
            }
        }
        if (availableItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 12.dp, end = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(CoreUiR.string.ui_copy_2, availableItems.size),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showAvailableItems = !showAvailableItems }) {
                        Text(
                            stringResource(
                                if (showAvailableItems) CoreUiR.string.core_ui_hide else CoreUiR.string.core_ui_show,
                            ),
                        )
                    }
                }
            }
            if (showAvailableItems) {
                items(availableItems, key = OptionPickerItem::id) { item ->
                    AvailableOptionRow(item)
                }
                onOpenAvailableItems?.let { openAvailableItems ->
                    item {
                        TextButton(
                            onClick = openAvailableItems,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        ) { Text(stringResource(CoreUiR.string.core_ui_installed_option_list_1)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledOptionRow(
    item: OptionPickerItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.tertiary,
                unselectedColor = MaterialTheme.colorScheme.outline,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            )
            item.supportingText.takeIf(String::isNotBlank)?.let { supportingText ->
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun AvailableOptionRow(item: OptionPickerItem) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = listOf(item.supportingText, "Download in Models")
                .filter(String::isNotBlank)
                .joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
    }
}
