package com.dmitriim.localaiplayground.feature.voice.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.feature.voice.presentation.VoicePhase
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceUiState

@Composable
internal fun VoiceTurnContent(
    state: VoiceUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCancel: () -> Unit,
    onNewConversation: () -> Unit,
) {
    VoiceTurnControls(state, onStartListening, onStopListening, onCancel, onNewConversation)
    state.level?.let { level ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recording ${formatDuration(level.elapsedMs)}", style = MaterialTheme.typography.titleMedium)
                Text("Input level: ${(level.rms * 100).toInt()}% RMS · ${(level.peak * 100).toInt()}% peak")
                Text("This Whisper profile publishes final text after you stop recording.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (state.finalTranscript.isNotBlank() || state.streamingResponse.isNotBlank()) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.finalTranscript.isNotBlank()) {
                    Text("You", style = MaterialTheme.typography.labelLarge)
                    Text(state.finalTranscript)
                }
                if (state.streamingResponse.isNotBlank()) {
                    Text("Assistant", style = MaterialTheme.typography.labelLarge)
                    Text(state.streamingResponse)
                }
            }
        }
    }
    state.contextUsage?.let { usage ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("In-memory conversation context", style = MaterialTheme.typography.titleMedium)
                Text("Prompt: ${usage.promptTokens} / ${usage.contextSize} tokens · reserved output: ${usage.reservedOutputTokens}")
                Text(
                    if (usage.omittedTurnCount == 0) "All prior turns fit in the selected context."
                    else "${usage.omittedTurnCount} oldest turn(s) were omitted for this response.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (state.conversation.isNotEmpty()) {
        Text("Conversation: ${state.conversation.size} completed turn(s) retained in memory.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun VoiceTurnControls(
    state: VoiceUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCancel: () -> Unit,
    onNewConversation: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        when (state.phase) {
            VoicePhase.LISTENING -> Button(onClick = onStopListening) { Text("Stop recording") }
            VoicePhase.IDLE, VoicePhase.ERROR -> {
                Button(onClick = onStartListening, enabled = state.canStart) { Text("Start push-to-talk") }
                if (state.conversation.isNotEmpty()) TextButton(onClick = onNewConversation) { Text("New conversation") }
            }
            VoicePhase.FINALIZING, VoicePhase.THINKING, VoicePhase.SPEAKING, VoicePhase.CANCELLING -> {
                Button(onClick = onCancel, enabled = state.phase != VoicePhase.CANCELLING) { Text("Cancel") }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String = "%.2fs".format(durationMs / 1_000.0)
