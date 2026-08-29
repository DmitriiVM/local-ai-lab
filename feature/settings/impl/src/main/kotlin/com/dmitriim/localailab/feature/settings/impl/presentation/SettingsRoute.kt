package com.dmitriim.localailab.feature.settings.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.feature.settings.impl.presentation.ui.SettingsScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun SettingsRoute(
    onOpenDeviceAndRuntimes: () -> Unit,
    viewModel: SettingsViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onOpenDeviceAndRuntimes = onOpenDeviceAndRuntimes,
        onUpdate = viewModel::update,
        onClearTemporaryMedia = viewModel::clearTemporaryMedia,
        onRequestClearRunHistory = viewModel::requestClearRunHistory,
        onDismissClearRunHistory = viewModel::dismissClearRunHistory,
        onClearRunHistory = viewModel::clearRunHistory,
        onRequestHuggingFaceToken = viewModel::requestHuggingFaceToken,
        onDismissHuggingFaceToken = viewModel::dismissHuggingFaceToken,
        onSaveHuggingFaceToken = viewModel::saveHuggingFaceToken,
        onClearHuggingFaceToken = viewModel::clearHuggingFaceToken,
    )
}
