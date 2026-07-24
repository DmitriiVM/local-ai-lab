package com.dmitriim.localaiplayground.feature.stt.presentation

import androidx.compose.runtime.Composable
import com.dmitriim.localaiplayground.feature.stt.presentation.ui.SpeechToTextScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun SpeechToTextRoute(viewModel: SpeechToTextViewModel = metroViewModel()) {
    SpeechToTextScreen()
}
