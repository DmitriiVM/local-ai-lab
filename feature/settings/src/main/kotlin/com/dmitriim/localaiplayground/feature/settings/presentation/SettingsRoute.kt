package com.dmitriim.localaiplayground.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localaiplayground.feature.settings.presentation.ui.SettingsScreen
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
    )
}
