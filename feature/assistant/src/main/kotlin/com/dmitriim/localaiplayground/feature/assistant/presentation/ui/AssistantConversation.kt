package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.feature.assistant.presentation.ChatMessage
import com.dmitriim.localaiplayground.feature.assistant.presentation.ChatMessageRole

@Composable
internal fun AssistantConversation(
    messages: List<ChatMessage>,
    modifier: Modifier,
    onCopy: (String) -> Unit,
    onEdit: (String) -> Unit,
    onRegenerate: () -> Unit,
    onSpeak: (String) -> Unit,
    canSpeak: Boolean,
    canRegenerate: Boolean,
    header: @Composable () -> Unit,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (message.role == ChatMessageRole.USER) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (message.role == ChatMessageRole.USER) "You" else "Assistant",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (message.streaming) CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), strokeWidth = 2.dp)
                if (message.failed) Text("Incomplete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
            Text(if (message.content.isBlank() && message.streaming) "Generating…" else message.content)
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                ) {
                    MessageActionButton(
                        icon = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy message",
                        enabled = message.content.isNotBlank(),
                        onClick = { onCopy(message.content) },
                    )
                    onRegenerate?.let { regenerate ->
                        MessageActionButton(
                            icon = Icons.Outlined.Replay,
                            contentDescription = "Regenerate response",
                            onClick = regenerate,
                        )
                    }
                    onSpeak?.let { speak ->
                        MessageActionButton(
                            icon = Icons.Outlined.VolumeUp,
                            contentDescription = "Speak response",
                            onClick = speak,
                        )
                    }
                    if (message.role == ChatMessageRole.USER) {
                        MessageActionButton(
                            icon = Icons.Outlined.Edit,
                            contentDescription = "Edit and retry",
                            onClick = { onEdit(message.id) },
                        )
                    }
                }
            }
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
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
        )
    }
}
