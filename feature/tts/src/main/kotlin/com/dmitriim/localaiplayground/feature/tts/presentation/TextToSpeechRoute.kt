package com.dmitriim.localaiplayground.feature.tts.presentation

import androidx.compose.runtime.Composable
import com.dmitriim.localaiplayground.feature.tts.presentation.ui.TextToSpeechScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun TextToSpeechRoute(viewModel: TextToSpeechViewModel = metroViewModel()) {
    TextToSpeechScreen()
}
