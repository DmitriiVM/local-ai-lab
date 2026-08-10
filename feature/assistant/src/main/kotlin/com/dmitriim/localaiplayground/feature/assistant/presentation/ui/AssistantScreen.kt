package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.core.ui.layout.LocalAppDimensions
import com.dmitriim.localaiplayground.feature.assistant.presentation.AssistantInputMode
import com.dmitriim.localaiplayground.feature.assistant.presentation.AssistantUiState
import com.dmitriim.localaiplayground.feature.assistant.presentation.ChatSettings
import com.dmitriim.localaiplayground.feature.assistant.presentation.SpeechInputSettings
import com.dmitriim.localaiplayground.feature.assistant.presentation.SpeechOutputSettings

@Composable
fun AssistantScreen(
    uiState: AssistantUiState,
    onUpdateInput: (String) -> Unit,
    onSelectMode: (AssistantInputMode) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSend: () -> Unit,
    onProfile: () -> Unit,
    onCancel: () -> Unit,
    onRegenerate: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onClearConversation: () -> Unit,
    onEditAndRetry: (String) -> Unit,
    onApplyChatSettings: (ModelId, ChatSettings) -> String?,
    onApplySpeechInputSettings: (ModelId, SpeechInputSettings) -> String?,
    onApplySpeechOutputSettings: (ModelId, String, SpeechOutputSettings) -> String?,
    onPreviewVoice: (ModelId, String, SpeechOutputSettings) -> String?,
    onUnloadChatModel: () -> Unit,
    onOpenModels: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    var activeSheet by remember { mutableStateOf<AssistantSettingsSheet?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val systemNavigationPadding = if (dimensions.bottomNavigationOverlayClearance == 0.dp) {
        Modifier.navigationBarsPadding()
    } else {
        Modifier
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(systemNavigationPadding)
                .imePadding()
                .padding(
                    start = dimensions.screenPadding,
                    top = dimensions.topBarOverlayClearance + 44.dp,
                    end = dimensions.screenPadding,
                    bottom = dimensions.bottomNavigationOverlayClearance + 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistantConfigurationBar(
                state = uiState,
                onOpenChat = { activeSheet = AssistantSettingsSheet.CHAT },
                onOpenListen = { activeSheet = AssistantSettingsSheet.LISTEN },
                onOpenSpeak = { activeSheet = AssistantSettingsSheet.SPEAK },
            )
            AssistantConversation(
                messages = uiState.messages,
                modifier = Modifier.weight(1f),
                onCopy = { clipboard.setText(AnnotatedString(it)) },
                onEdit = onEditAndRetry,
                onRegenerate = onRegenerate,
                onSpeak = onSpeakMessage,
                canSpeak = uiState.isIdle && uiState.selectedVoice != null,
                canRegenerate = uiState.isIdle,
                header = {
                    uiState.errorMessage?.let { message -> StatusMessage("Assistant needs attention", message) }
                },
                footer = { uiState.metrics?.let { metrics -> ChatMetricsCard(metrics, uiState.contextUsage) } },
            )
            AssistantComposer(
                state = uiState,
                onInput = onUpdateInput,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onSend = onSend,
                onProfile = onProfile,
                onCancel = onCancel,
            )
        }
        AssistantToolbarActions(
            state = uiState,
            onClearConversation = { showClearConfirmation = true },
            onOpenSettings = { activeSheet = AssistantSettingsSheet.INPUT_MODE },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = dimensions.screenPadding),
        )
    }

    when (activeSheet) {
        AssistantSettingsSheet.CHAT -> AssistantChatSettingsSheet(
            models = uiState.chatModels,
            selectedModelId = uiState.selectedChatModelId,
            settings = uiState.chatSettings,
            enabled = uiState.isIdle,
            onApply = onApplyChatSettings,
            onUnload = onUnloadChatModel,
            onOpenModels = onOpenModels,
            onDismiss = { activeSheet = null },
        )
        AssistantSettingsSheet.LISTEN -> AssistantListenSettingsSheet(
            models = uiState.speechModels,
            selectedModelId = uiState.selectedSpeechModelId,
            settings = uiState.speechInputSettings,
            enabled = uiState.isIdle,
            onApply = onApplySpeechInputSettings,
            onOpenModels = onOpenModels,
            onDismiss = { activeSheet = null },
        )
        AssistantSettingsSheet.SPEAK -> AssistantSpeakSettingsSheet(
            models = uiState.voiceModels,
            selectedModelId = uiState.selectedVoiceModelId,
            selectedVoiceId = uiState.selectedVoiceId,
            settings = uiState.speechOutputSettings,
            enabled = uiState.isIdle,
            onApply = onApplySpeechOutputSettings,
            onPreview = onPreviewVoice,
            onOpenModels = onOpenModels,
            onDismiss = { activeSheet = null },
        )
        AssistantSettingsSheet.INPUT_MODE -> AssistantInputModeSettingsSheet(
            selectedMode = uiState.inputMode,
            enabled = uiState.isIdle,
            onSelectMode = onSelectMode,
            onDismiss = { activeSheet = null },
        )
        null -> Unit
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear this conversation?") },
            text = { Text("This removes all messages and metrics from this Assistant conversation.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearConversation()
                        showClearConfirmation = false
                    },
                ) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") } },
        )
    }
}

private enum class AssistantSettingsSheet { CHAT, LISTEN, SPEAK, INPUT_MODE }
