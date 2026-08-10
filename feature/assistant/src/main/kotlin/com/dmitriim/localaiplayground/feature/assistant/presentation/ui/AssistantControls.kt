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
import androidx.compose.ui.res.stringResource
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR

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
            title = stringResource(CoreUiR.string.ui_copy_8),
            value = state.selectedChatModel?.displayName ?: "Not configured",
            onClick = onOpenChat,
            ready = state.selectedChatModel?.installed == true,
            icon = { Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f),
        )
        ConfigurationButton(
            title = stringResource(CoreUiR.string.ui_copy_9),
            value = state.selectedSpeechModel?.displayName ?: "Not configured",
            onClick = onOpenListen,
            ready = state.selectedSpeechModel?.installed == true,
            icon = { Icon(Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f),
        )
        ConfigurationButton(
            title = stringResource(CoreUiR.string.ui_copy_10),
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
            Text(stringResource(CoreUiR.string.assistant_assistant_controls_4))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(CoreUiR.string.ui_copy_11))
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
                label = { Text(stringResource(CoreUiR.string.assistant_assistant_controls_5)) },
                placeholder = { Text(stringResource(CoreUiR.string.assistant_assistant_controls_6)) },
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
                        TextButton(onClick = onProfile, enabled = state.canSend) { Text(stringResource(CoreUiR.string.assistant_assistant_controls_7)) }
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
                            Icon(Icons.Outlined.Mic, contentDescription = stringResource(CoreUiR.string.ui_copy_12))
                        }
                        AssistantPrimaryActionButton(
                            onClick = onSend,
                            enabled = state.canSend,
                            purpleTonal = true,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(CoreUiR.string.ui_copy_13),
                            )
                        }
                    }
                    AssistantOperation.Recording -> AssistantPrimaryActionButton(onClick = onStopRecording) {
                        Icon(Icons.Outlined.Stop, contentDescription = stringResource(CoreUiR.string.ui_copy_14))
                    }
                    AssistantOperation.Cancelling -> AssistantPrimaryActionButton(onClick = {}, enabled = false) {
                        Icon(Icons.Outlined.Stop, contentDescription = stringResource(CoreUiR.string.ui_copy_15))
                    }
                    else -> {
                        val llmOperation = state.operation == AssistantOperation.Loading ||
                            state.operation == AssistantOperation.Generating
                        if (!llmOperation || state.selectedChatModel?.capabilities?.cancellation == true) {
                            AssistantPrimaryActionButton(onClick = onCancel) {
                                Icon(Icons.Outlined.Close, contentDescription = stringResource(CoreUiR.string.ui_copy_16))
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
            level != null -> Text(stringResource(CoreUiR.string.assistant_assistant_controls_format_1, "%.1f".format(level.elapsedMs / 1_000.0), (level.peak * 100).toInt()),
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

@Composable
private fun operationLabel(operation: AssistantOperation): String = when (operation) {
    AssistantOperation.Idle -> ""
    AssistantOperation.Recording -> stringResource(CoreUiR.string.assistant_operation_listening)
    AssistantOperation.Transcribing -> stringResource(CoreUiR.string.assistant_operation_transcribing)
    AssistantOperation.Loading -> stringResource(CoreUiR.string.assistant_operation_loading_model)
    AssistantOperation.Generating -> stringResource(CoreUiR.string.assistant_generating)
    AssistantOperation.Speaking -> stringResource(CoreUiR.string.assistant_operation_speaking)
    AssistantOperation.Cancelling -> stringResource(CoreUiR.string.assistant_operation_stopping)
}
