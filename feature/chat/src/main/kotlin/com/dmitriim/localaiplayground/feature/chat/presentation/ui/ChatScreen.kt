package com.dmitriim.localaiplayground.feature.chat.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatOperation
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatSettings
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatUiState

/** Coordinates durable chat state with the independently reusable screen sections. */
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onSelectModel: (ModelId) -> Unit,
    onUpdateInput: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onRegenerate: () -> Unit,
    onUnloadModel: () -> Unit,
    onClearConversation: () -> Unit,
    onEditAndRetry: (String) -> Unit,
    onUpdateSettings: ((ChatSettings) -> ChatSettings) -> Unit,
    onResetSettings: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    var showSettings by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val systemNavigationPadding = if (dimensions.bottomNavigationOverlayClearance == 0.dp) {
        Modifier.navigationBarsPadding()
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(systemNavigationPadding)
            .imePadding()
            .padding(
                start = 16.dp,
                top = dimensions.topBarOverlayClearance + 12.dp,
                end = 16.dp,
                bottom = dimensions.bottomNavigationOverlayClearance + 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChatModelSelector(
            state = uiState,
            enabled = uiState.operation == ChatOperation.IDLE,
            onSelect = onSelectModel,
        )
        ChatConversation(
            messages = uiState.messages,
            modifier = Modifier.weight(1f),
            onCopy = { clipboard.setText(AnnotatedString(it)) },
            onEdit = onEditAndRetry,
            onRegenerate = onRegenerate,
            canRegenerate = uiState.operation == ChatOperation.IDLE,
            header = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChatActionRow(
                        state = uiState,
                        onShowSettings = { showSettings = true },
                        onUnload = onUnloadModel,
                        onClear = { showClearConfirmation = true },
                    )
                    uiState.contextUsage?.let { usage ->
                        val omitted = if (usage.omittedMessageCount > 0) {
                            " ${usage.omittedMessageCount} earlier message(s) omitted."
                        } else {
                            ""
                        }
                        Text(
                            "Context budget: ${usage.promptTokens} input · " +
                                "${usage.reservedOutputTokens} max output · ${usage.contextSize} total.$omitted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    uiState.errorMessage?.let { message ->
                        StatusMessage(title = "Chat needs attention", explanation = message)
                    }
                }
            },
            footer = {
                uiState.metrics?.let { metrics -> ChatMetricsCard(metrics) }
            },
        )
        ChatComposer(
            state = uiState,
            onInput = onUpdateInput,
            onSend = onSend,
            onStop = onStop,
        )
    }

    if (showSettings) {
        ChatSettingsDialog(
            settings = uiState.settings,
            onSettingsChange = onUpdateSettings,
            onReset = onResetSettings,
            onDismiss = { showSettings = false },
        )
    }
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear this conversation?") },
            text = { Text("This removes the in-memory messages and metrics. Stage 7 adds durable conversation history.") },
            confirmButton = {
                TextButton(onClick = { onClearConversation(); showClearConfirmation = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") } },
        )
    }
}
