package com.dmitriim.localailab.feature.device.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.feature.device.presentation.ui.DeviceScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun DeviceRoute(viewModel: DeviceViewModel = metroViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeviceScreen(
        state = state,
        onRefresh = viewModel::refresh,
    )
}
