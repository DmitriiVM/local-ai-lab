package com.dmitriim.localaiplayground.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.dmitriim.localaiplayground.feature.settings.presentation.ui.SettingsScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = metroViewModel()) {
    SettingsScreen()
}
