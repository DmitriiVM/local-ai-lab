package com.dmitriim.localaiplayground.feature.chat.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatMessage
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatMessageRole

@Composable
internal fun ChatConversation(
    messages: List<ChatMessage>,
    modifier: Modifier,
    onCopy: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (messages.isNotEmpty() && (lastVisible >= messages.lastIndex - 1 || lastVisible == -1)) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    if (messages.isEmpty()) {
        StatusMessage(
            title = "Start a local conversation",
            explanation = "Your messages and responses stay on this device. The current conversation is kept only in memory.",
        )
    } else {
        LazyColumn(state = listState, modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { message ->
                ChatMessageCard(message, onCopy, onEdit)
            }
        }
    }
}

@Composable
private fun ChatMessageCard(message: ChatMessage, onCopy: (String) -> Unit, onEdit: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onCopy(message.content) }, enabled = message.content.isNotBlank()) { Text("Copy") }
                if (message.role == ChatMessageRole.USER) {
                    TextButton(onClick = { onEdit(message.id) }) { Text("Edit & retry") }
                }
            }
        }
    }
}
