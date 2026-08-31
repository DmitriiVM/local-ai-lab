package com.dmitriim.localailab.feature.assistant.impl.presentation.ui.chat

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.style.AppSurfaceStyle
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatMessage
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatMessageRole

@Composable
internal fun AssistantConversation(
    messages: List<ChatMessage>,
    onCopy: (String) -> Unit,
    onEdit: (String) -> Unit,
    onRegenerate: () -> Unit,
    onSpeak: (String) -> Unit,
    canSpeak: Boolean,
    canRegenerate: Boolean,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val wasNearBottom = lastVisible == -1 ||
            lastVisible >= listState.layoutInfo.totalItemsCount - 2
        if (messages.isNotEmpty() && wasNearBottom) {
            listState.animateScrollToItem(messages.lastIndex + 1)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "chat-header") {
            header()
        }
        if (messages.isNotEmpty()) {
            items(messages, key = { it.id }) { message ->
                ChatMessageCard(
                    message = message,
                    onCopy = onCopy,
                    onEdit = onEdit,
                    onRegenerate = onRegenerate.takeIf {
                        canRegenerate &&
                            message.role == ChatMessageRole.ASSISTANT &&
                            message.id == messages.lastOrNull { it.role == ChatMessageRole.ASSISTANT }?.id
                    },
                    onSpeak = if (
                        canSpeak &&
                        message.role == ChatMessageRole.ASSISTANT &&
                        !message.streaming &&
                        message.content.isNotBlank()
                    ) {
                        { onSpeak(message.id) }
                    } else {
                        null
                    },
                )
            }
        }
        item(key = "chat-footer") {
            footer()
        }
    }
}

@Composable
private fun ChatMessageCard(
    message: ChatMessage,
    onCopy: (String) -> Unit,
    onEdit: (String) -> Unit,
    onRegenerate: (() -> Unit)?,
    onSpeak: (() -> Unit)?,
) {
    val isUserMessage = message.role == ChatMessageRole.USER
    val colors = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isUserMessage) {
                    Modifier
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = colors.tertiary.copy(alpha = 0.20f),
                        shape = cardShape,
                    )
                },
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isUserMessage) {
                AppSurfaceStyle.tonalCardColor(colors)
            } else {
                colors.surfaceContainer.copy(alpha = 0.96f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MessageCardHeader(message, isUserMessage)
            Text(
                text = if (message.content.isBlank() && message.streaming) {
                    stringResource(CoreUiR.string.assistant_generating)
                } else {
                    message.content
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            MessageActions(message, onCopy, onEdit, onRegenerate, onSpeak)
        }
    }
}

@Composable
private fun MessageCardHeader(message: ChatMessage, isUserMessage: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MessageRoleLabel(isUserMessage)
        Spacer(modifier = Modifier.weight(1f))
        if (message.streaming) {
            CircularProgressIndicator(
                modifier = Modifier
                    .height(16.dp)
                    .width(16.dp),
                strokeWidth = 2.dp,
            )
        }
        if (message.failed) {
            Text(
                text = stringResource(CoreUiR.string.assistant_assistant_conversation_8),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun MessageActions(
    message: ChatMessage,
    onCopy: (String) -> Unit,
    onEdit: (String) -> Unit,
    onRegenerate: (() -> Unit)?,
    onSpeak: (() -> Unit)?,
) = CompositionLocalProvider(
    LocalMinimumInteractiveComponentSize provides 32.dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
    ) {
        MessageActionButton(
            icon = Icons.Outlined.ContentCopy,
            contentDescription = stringResource(CoreUiR.string.ui_copy_17),
            onClick = { onCopy(message.content) },
            enabled = message.content.isNotBlank(),
        )
        onRegenerate?.let { onRegenerate ->
            MessageActionButton(
                icon = Icons.Outlined.Replay,
                contentDescription = stringResource(CoreUiR.string.ui_copy_18),
                onClick = onRegenerate,
            )
        }
        onSpeak?.let { onSpeak ->
            MessageActionButton(
                icon = Icons.Outlined.VolumeUp,
                contentDescription = stringResource(CoreUiR.string.ui_copy_19),
                onClick = onSpeak,
            )
        }
        if (message.role == ChatMessageRole.USER) {
            MessageActionButton(
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(CoreUiR.string.ui_copy_20),
                onClick = { onEdit(message.id) },
            )
        }
    }
}

@Composable
private fun MessageRoleLabel(isUserMessage: Boolean) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (isUserMessage) {
        colors.tertiaryContainer.copy(alpha = 0.82f)
    } else {
        colors.tertiary.copy(alpha = 0.16f)
    }
    val contentColor = if (isUserMessage) colors.onTertiaryContainer else colors.tertiary
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isUserMessage) {
                    Icons.Outlined.Person
                } else {
                    Icons.AutoMirrored.Outlined.Chat
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(
                    if (isUserMessage) {
                        CoreUiR.string.assistant_you
                    } else {
                        CoreUiR.string.assistant_assistant
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MessageActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
        )
    }
}
