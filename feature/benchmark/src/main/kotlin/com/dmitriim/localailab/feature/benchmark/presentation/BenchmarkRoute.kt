package com.dmitriim.localailab.feature.benchmark.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.feature.benchmark.presentation.ui.BenchmarkScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun BenchmarkRoute(viewModel: BenchmarkLabViewModel = metroViewModel()) {
    DisposableEffect(viewModel) {
        viewModel.runtimeLeaseController.onVisible()
        onDispose(viewModel.runtimeLeaseController::onHidden)
    }
    val state = viewModel.state.collectAsStateWithLifecycle().value
    BenchmarkScreen(
        state = state,
        onWarmupsChange = viewModel::setWarmupIterations,
        onMeasuredChange = viewModel::setMeasuredIterations,
        onToggleStartupMode = viewModel::toggleStartupMode,
        onStart = viewModel::start,
        onCancel = viewModel::cancel,
    )
}
