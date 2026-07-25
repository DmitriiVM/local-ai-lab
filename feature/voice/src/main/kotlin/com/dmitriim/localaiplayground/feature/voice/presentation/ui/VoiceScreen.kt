package com.dmitriim.localaiplayground.feature.voice.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceLanguage
import com.dmitriim.localaiplayground.feature.voice.presentation.VoicePhase
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceSettings
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceUiState

@Composable
fun VoiceScreen(
    state: VoiceUiState,
    onSelectSpeechModel: (ModelId) -> Unit,
    onSelectChatModel: (ModelId) -> Unit,
    onSelectVoiceModel: (ModelId) -> Unit,
    onSelectLanguage: (VoiceLanguage) -> Unit,
    onUpdateSettings: ((VoiceSettings) -> VoiceSettings) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCancel: () -> Unit,
    onNewConversation: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val busy = state.phase !in setOf(VoicePhase.IDLE, VoicePhase.ERROR)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = dimensions.topBarOverlayClearance + 12.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Local voice assistant", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Push to talk: record a completed turn, transcribe it locally, generate a complete response, then synthesize and play it. Automatic endpointing and spoken barge-in are not enabled in this MVP.",
            style = MaterialTheme.typography.bodyMedium,
        )
        VoicePipelineConfiguration(
            state = state,
            enabled = !busy,
            onSelectSpeechModel = onSelectSpeechModel,
            onSelectChatModel = onSelectChatModel,
            onSelectVoiceModel = onSelectVoiceModel,
            onSelectLanguage = onSelectLanguage,
            onUpdateSettings = onUpdateSettings,
        )
        state.configurationError?.let { StatusMessage("Pipeline configuration required", it) }
        state.statusMessage?.let { StatusMessage(state.phase.label, it) }
        state.errorMessage?.let { StatusMessage("Voice assistant needs attention", it) }
        VoiceTurnContent(
            state = state,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onCancel = onCancel,
            onNewConversation = onNewConversation,
        )
        state.metrics?.let { VoiceMetricsCard(it) }
    }
}
