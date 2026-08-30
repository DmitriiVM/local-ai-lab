package com.dmitriim.localailab.feature.assistant.impl.presentation.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.StatusMessage
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantInputMode
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantUiState
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechInputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.chat.AssistantChatSettingsSheet
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.chat.AssistantConversation
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.chat.ChatMetricsCard
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.stt.AssistantListenSettingsSheet
import com.dmitriim.localailab.feature.assistant.impl.presentation.ui.tts.AssistantSpeakSettingsSheet

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
                    val message = uiState.errorMessage
                        ?: uiState.voiceConfigurationError?.let { stringResource(it) }
                    message?.let {
                        StatusMessage(stringResource(CoreUiR.string.assistant_needs_attention), it)
                    }
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

    AssistantSettingsSheets(
        activeSheet = activeSheet,
        uiState = uiState,
        onSelectMode = onSelectMode,
        onApplyChatSettings = onApplyChatSettings,
        onApplySpeechInputSettings = onApplySpeechInputSettings,
        onApplySpeechOutputSettings = onApplySpeechOutputSettings,
        onPreviewVoice = onPreviewVoice,
        onUnloadChatModel = onUnloadChatModel,
        onOpenModels = onOpenModels,
    ) { activeSheet = null }
    ClearConversationDialog(
        show = showClearConfirmation,
        onDismiss = { showClearConfirmation = false },
    ) {
        onClearConversation()
        showClearConfirmation = false
    }
}

@Composable
private fun AssistantSettingsSheets(
    activeSheet: AssistantSettingsSheet?,
    uiState: AssistantUiState,
    onSelectMode: (AssistantInputMode) -> Unit,
    onApplyChatSettings: (ModelId, ChatSettings) -> String?,
    onApplySpeechInputSettings: (ModelId, SpeechInputSettings) -> String?,
    onApplySpeechOutputSettings: (ModelId, String, SpeechOutputSettings) -> String?,
    onPreviewVoice: (ModelId, String, SpeechOutputSettings) -> String?,
    onUnloadChatModel: () -> Unit,
    onOpenModels: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (activeSheet) {
        AssistantSettingsSheet.CHAT -> {
            AssistantChatSettingsSheet(
                models = uiState.chatModels,
                selectedModelId = uiState.selectedChatModelId,
                settings = uiState.chatSettings,
                enabled = uiState.isIdle,
                onApply = onApplyChatSettings,
                onUnload = onUnloadChatModel,
                onOpenModels = onOpenModels,
                onDismiss = onDismiss,
            )
        }

        AssistantSettingsSheet.LISTEN -> {
            AssistantListenSettingsSheet(
                models = uiState.speechModels,
                selectedModelId = uiState.selectedSpeechModelId,
                settings = uiState.speechInputSettings,
                enabled = uiState.isIdle,
                onApply = onApplySpeechInputSettings,
                onOpenModels = onOpenModels,
                onDismiss = onDismiss,
            )
        }

        AssistantSettingsSheet.SPEAK -> {
            AssistantSpeakSettingsSheet(
                models = uiState.voiceModels,
                selectedModelId = uiState.selectedVoiceModelId,
                selectedVoiceId = uiState.selectedVoiceId,
                settings = uiState.speechOutputSettings,
                enabled = uiState.isIdle,
                onApply = onApplySpeechOutputSettings,
                onPreview = onPreviewVoice,
                onOpenModels = onOpenModels,
                onDismiss = onDismiss,
            )
        }

        AssistantSettingsSheet.INPUT_MODE -> {
            AssistantInputModeSettingsSheet(
                selectedMode = uiState.inputMode,
                enabled = uiState.isIdle,
                onSelectMode = onSelectMode,
                onDismiss = onDismiss,
            )
        }

        null -> Unit
    }
}

@Composable
private fun ClearConversationDialog(show: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(CoreUiR.string.assistant_assistant_screen_16)) },
            text = { Text(stringResource(CoreUiR.string.assistant_assistant_screen_17)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    content = { Text(stringResource(CoreUiR.string.assistant_assistant_screen_18)) },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    content = {
                        Text(stringResource(CoreUiR.string.assistant_assistant_screen_19))
                    },
                )
            },
        )
    }
}

private enum class AssistantSettingsSheet { CHAT, LISTEN, SPEAK, INPUT_MODE }
