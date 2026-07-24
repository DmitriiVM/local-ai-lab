package com.dmitriim.localaiplayground.feature.runs.presentation

import androidx.compose.runtime.Composable
import com.dmitriim.localaiplayground.feature.runs.presentation.ui.RunsScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun RunsRoute(viewModel: RunsViewModel = metroViewModel()) {
    RunsScreen()
}
