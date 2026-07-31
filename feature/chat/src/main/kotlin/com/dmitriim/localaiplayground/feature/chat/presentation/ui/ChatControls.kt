package com.dmitriim.localaiplayground.feature.chat.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatMessageRole
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
    Column {
        Text("Model", style = MaterialTheme.typography.labelLarge)
        Button(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.displayName ?: "Choose model")
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
        if (state.availableModels.none { it.installed }) {
            Text(
                "Download a chat model from the Models tab before starting a conversation.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun ChatActionRow(
    state: ChatUiState,
    onShowSettings: () -> Unit,
    onRegenerate: () -> Unit,
    onUnload: () -> Unit,
    onClear: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onShowSettings, enabled = state.operation == ChatOperation.IDLE) { Text("Settings") }
        OutlinedButton(
            onClick = onRegenerate,
            enabled = state.operation == ChatOperation.IDLE && state.messages.any { it.role == ChatMessageRole.ASSISTANT },
        ) { Text("Regenerate") }
        OutlinedButton(onClick = onUnload, enabled = state.operation == ChatOperation.IDLE) { Text("Unload") }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onClear, enabled = state.operation == ChatOperation.IDLE && state.messages.isNotEmpty()) { Text("Clear") }
    }
}

@Composable
internal fun ChatComposer(state: ChatUiState, onInput: (String) -> Unit, onSend: () -> Unit, onStop: () -> Unit) {
    val active = state.operation != ChatOperation.IDLE
    OutlinedTextField(
        value = state.input,
        onValueChange = onInput,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Message") },
        minLines = 2,
        maxLines = 6,
        enabled = !active,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() }),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSend,
            enabled = !active &&
                state.input.isNotBlank() &&
                state.availableModels.any { it.id == state.selectedModelId && it.installed },
        ) { Text("Send") }
        if (active) {
            OutlinedButton(onClick = onStop) { Text(if (state.operation == ChatOperation.CANCELLING) "Stopping…" else "Stop") }
        }
        if (state.operation == ChatOperation.LOADING) Text("Loading local model…", style = MaterialTheme.typography.bodySmall)
    }
}
