package com.dmitriim.localaiplayground.feature.stt.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.result.StatusMessage

@Composable
fun SpeechToTextScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusMessage(
            title = "Speech model required",
            explanation = "The sherpa-onnx runtime is checked locally. A compatible speech " +
                "recognition model must be imported before recording or file transcription.",
        )
    }
}
