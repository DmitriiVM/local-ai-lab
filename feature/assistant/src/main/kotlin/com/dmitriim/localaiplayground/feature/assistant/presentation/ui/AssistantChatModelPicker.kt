package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.feature.assistant.presentation.ChatModelOption

@Composable
internal fun AssistantChatModelPicker(
    models: List<ChatModelOption>,
    selectedId: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val selected = models.firstOrNull { it.id.value == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Chat model", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = onClick,
            enabled = enabled && models.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selected?.displayName ?: "Install a chat model in Models",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
    val installedModels = models.filter(ChatModelOption::installed)
    val availableModels = models.filterNot(ChatModelOption::installed)
    var showAvailableModels by remember { mutableStateOf(false) }
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
            TextButton(onClick = onBack) { Text("Back") }
            Text("Chat model", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = "Choose an installed model for private local chat.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 12.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
        ) {
            item {
                Text(
                    text = "Installed",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            if (installedModels.isEmpty()) {
                item {
                    Text(
                        text = "No chat models are installed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(installedModels, key = { it.id.value }) { model ->
                    AssistantChatModelRow(
                        model = model,
                        selected = model.id == selectedId,
                        enabled = enabled,
                        onClick = { onSelect(model.id) },
                    )
                }
            }
            if (availableModels.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 12.dp, end = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Available in Models (${availableModels.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { showAvailableModels = !showAvailableModels }) {
                            Text(if (showAvailableModels) "Hide" else "Show")
                        }
                    }
                }
                if (showAvailableModels) {
                    items(availableModels, key = { it.id.value }) { model ->
                        UnavailableAssistantChatModelRow(model)
                    }
                    item {
                        TextButton(
                            onClick = onOpenModels,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        ) { Text("Open Models") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantChatModelRow(
    model: ChatModelOption,
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
        Text(
            text = model.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun UnavailableAssistantChatModelRow(model: ChatModelOption) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = model.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Download in Models",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
    }
}
