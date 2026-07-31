package com.dmitriim.localaiplayground.feature.chat.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatOperation
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatUiState

@Composable
internal fun ChatModelSelector(state: ChatUiState, enabled: Boolean, onSelect: (ModelId) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.availableModels.firstOrNull { it.id == state.selectedModelId }
    if (state.availableModels.isEmpty()) {
        StatusMessage(
            title = "Chat model required",
            explanation = "No compatible GGUF chat models are available in this build.",
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Chat model",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        selected?.displayName ?: "Choose model",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("Change", style = MaterialTheme.typography.labelLarge)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (model.installed) {
                                    "${model.displayName} (${model.defaultContextSize} context)"
                                } else {
                                    "${model.displayName} · Download in Models"
                                },
                            )
                        },
                        onClick = { onSelect(model.id); expanded = false },
                        enabled = model.installed,
                    )
                }
            }
        }
        if (state.availableModels.none { it.installed }) {
            Text(
                "Download a chat model from the Models tab before starting a conversation.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ChatActionRow(
    state: ChatUiState,
    onShowSettings: () -> Unit,
    onUnload: () -> Unit,
    onClear: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(onClick = onShowSettings, enabled = state.operation == ChatOperation.IDLE) {
            Text("Generation")
        }
        TextButton(onClick = onUnload, enabled = state.operation == ChatOperation.IDLE) {
            Text("Unload model")
        }
        TextButton(
            onClick = onClear,
            enabled = state.operation == ChatOperation.IDLE && state.messages.isNotEmpty(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Clear chat")
        }
    }
}

@Composable
internal fun ChatComposer(state: ChatUiState, onInput: (String) -> Unit, onSend: () -> Unit, onStop: () -> Unit) {
    val active = state.operation != ChatOperation.IDLE
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInput,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Message") },
                placeholder = { Text("Ask this model…") },
                minLines = 1,
                maxLines = 4,
                enabled = !active,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() }),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.operation == ChatOperation.LOADING) {
                    Text(
                        "Loading local model…",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (active) {
                    OutlinedButton(onClick = onStop) {
                        Text(if (state.operation == ChatOperation.CANCELLING) "Stopping…" else "Stop")
                    }
                } else {
                    Button(
                        onClick = onSend,
                        enabled = state.input.isNotBlank() &&
                            state.availableModels.any { it.id == state.selectedModelId && it.installed },
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}
