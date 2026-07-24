package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage

@Composable
fun TextToSpeechScreen() {
    val dimensions = LocalAppDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 20.dp,
                top = dimensions.topBarOverlayClearance + 20.dp,
                end = 20.dp,
                bottom = 20.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusMessage(
            title = "Voice model required",
            explanation = "The sherpa-onnx runtime is checked locally. Import and validate a " +
                "compatible voice bundle before speech synthesis is enabled.",
        )
    }
}
