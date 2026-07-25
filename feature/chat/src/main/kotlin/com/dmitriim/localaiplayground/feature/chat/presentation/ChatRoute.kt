package com.dmitriim.localaiplayground.feature.chat.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localaiplayground.feature.chat.presentation.ui.ChatScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ChatRoute(
    viewModel: ChatViewModel = metroViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    ChatScreen(
        uiState = uiState,
        onSelectModel = viewModel::selectModel,
        onUpdateInput = viewModel::updateInput,
        onSend = viewModel::send,
        onStop = viewModel::stop,
        onRegenerate = viewModel::regenerate,
        onUnloadModel = viewModel::unloadModel,
        onClearConversation = viewModel::clearConversation,
        onEditAndRetry = viewModel::editAndRetry,
        onUpdateSettings = viewModel::updateSettings,
        onResetSettings = viewModel::resetSettings,
    )
}
