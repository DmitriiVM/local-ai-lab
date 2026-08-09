package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.feature.assistant.presentation.AssistantOperation
import com.dmitriim.localaiplayground.feature.assistant.presentation.AssistantUiState

@Composable
internal fun AssistantConfigurationBar(
    state: AssistantUiState,
    onOpenChat: () -> Unit,
    onOpenListen: () -> Unit,
    onOpenSpeak: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConfigurationButton(
            title = "Chat",
            value = state.selectedChatModel?.displayName ?: "Not configured",
            onClick = onOpenChat,
            ready = state.selectedChatModel?.installed == true,
            icon = { Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f),
        )
        ConfigurationButton(
            title = "Listen",
            value = state.selectedSpeechModel?.displayName ?: "Not configured",
            onClick = onOpenListen,
            ready = state.selectedSpeechModel?.installed == true,
            icon = { Icon(Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f),
        )
        ConfigurationButton(
            title = "Speak",
            value = state.selectedVoice?.displayName ?: "Not configured",
            onClick = onOpenSpeak,
            ready = state.selectedVoiceModel?.installed == true && state.selectedVoice != null,
            icon = { Icon(Icons.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun AssistantToolbarActions(
    state: AssistantUiState,
    onClearConversation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = onClearConversation,
            enabled = state.isIdle && state.messages.isNotEmpty(),
        ) {
            Text("Clear")
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Assistant settings")
        }
    }
}

@Composable
private fun ConfigurationButton(
    title: String,
    value: String,
    onClick: () -> Unit,
    ready: Boolean,
    icon: @Composable () -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                icon()
                Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Icon(
                    imageVector = if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = if (ready) "$title ready" else "$title not configured",
                    tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun AssistantComposer(
    state: AssistantUiState,
    onInput: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSend: () -> Unit,
    onProfile: () -> Unit,
    onCancel: () -> Unit,
) {
    val active = !state.isIdle
    val composerShape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                shape = composerShape,
            ),
        shape = composerShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInput,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                label = { Text("Message") },
                placeholder = { Text("Ask the assistant…") },
                minLines = 1,
                maxLines = 4,
                enabled = !active,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() }),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isIdle) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        TextButton(onClick = onProfile, enabled = state.canSend) { Text("Profile") }
                    }
                } else {
                    AssistantOperationStatus(
                        operation = state.operation,
                        level = state.level,
                        modifier = Modifier.weight(1f),
                    )
                }
                when (state.operation) {
                    AssistantOperation.Idle -> {
                        OutlinedIconButton(onClick = onStartRecording, enabled = state.canDictate) {
                            Icon(Icons.Outlined.Mic, contentDescription = "Record voice")
                        }
                        AssistantPrimaryActionButton(
                            onClick = onSend,
                            enabled = state.canSend,
                            purpleTonal = true,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Send message",
                            )
                        }
                    }
                    AssistantOperation.Recording -> AssistantPrimaryActionButton(onClick = onStopRecording) {
                        Icon(Icons.Outlined.Stop, contentDescription = "Stop recording")
                    }
                    AssistantOperation.Cancelling -> AssistantPrimaryActionButton(onClick = {}, enabled = false) {
                        Icon(Icons.Outlined.Stop, contentDescription = "Stopping")
                    }
                    else -> {
                        val llmOperation = state.operation == AssistantOperation.Loading ||
                            state.operation == AssistantOperation.Generating
                        if (!llmOperation || state.selectedChatModel?.capabilities?.cancellation == true) {
                            AssistantPrimaryActionButton(onClick = onCancel) {
                                Icon(Icons.Outlined.Close, contentDescription = "Cancel operation")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantPrimaryActionButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    purpleTonal: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (purpleTonal) colors.tertiaryContainer.copy(alpha = 0.78f) else colors.tertiary,
            contentColor = if (purpleTonal) colors.onTertiaryContainer else colors.onTertiary,
            disabledContainerColor = if (purpleTonal) {
                colors.tertiaryContainer.copy(alpha = 0.28f)
            } else {
                colors.onSurface.copy(alpha = 0.12f)
            },
            disabledContentColor = if (purpleTonal) {
                colors.onTertiaryContainer.copy(alpha = 0.38f)
            } else {
                colors.onSurface.copy(alpha = 0.38f)
            },
        ),
        content = content,
    )
}

@Composable
private fun AssistantOperationStatus(
    operation: AssistantOperation,
    level: com.dmitriim.localaiplayground.core.audio.input.model.AudioLevel?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        when {
            level != null -> Text(
                "Recording ${"%.1f".format(level.elapsedMs / 1_000.0)}s · ${(level.peak * 100).toInt()}% peak",
                style = MaterialTheme.typography.bodySmall,
            )
            operation !in setOf(AssistantOperation.Idle, AssistantOperation.Recording) -> Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(operationLabel(operation), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun operationLabel(operation: AssistantOperation): String = when (operation) {
    AssistantOperation.Idle -> ""
    AssistantOperation.Recording -> "Listening…"
    AssistantOperation.Transcribing -> "Transcribing…"
    AssistantOperation.Loading -> "Loading model…"
    AssistantOperation.Generating -> "Generating…"
    AssistantOperation.Speaking -> "Speaking…"
    AssistantOperation.Cancelling -> "Stopping…"
}
