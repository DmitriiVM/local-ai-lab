package com.dmitriim.localaiplayground.feature.voice.presentation

import androidx.compose.runtime.Composable
import com.dmitriim.localaiplayground.feature.voice.presentation.ui.VoiceScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun VoiceRoute(viewModel: VoiceViewModel = metroViewModel()) {
    VoiceScreen()
}
