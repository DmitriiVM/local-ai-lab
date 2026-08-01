package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal data class SettingsModelItem(
    val id: String,
    val name: String,
    val installed: Boolean,
)

@Composable
internal fun AssistantSettingsModelPicker(
    label: String,
    items: List<SettingsModelItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onOpenModels: () -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = items.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && items.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected?.name ?: "Not configured", maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(if (item.installed) item.name else "${item.name} · Not installed") },
                    onClick = {
                        expanded = false
                        onSelect(item.id)
                    },
                    enabled = item.installed,
                )
            }
        }
        if (items.any { !it.installed }) {
            TextButton(onClick = onOpenModels) { Text("Open Models") }
        }
    }
}
