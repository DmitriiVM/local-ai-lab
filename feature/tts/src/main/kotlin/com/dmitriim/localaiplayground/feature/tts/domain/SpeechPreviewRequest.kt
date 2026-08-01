package com.dmitriim.localaiplayground.feature.tts.domain

import com.dmitriim.localaiplayground.core.model.manifest.ModelId

data class SpeechPreviewRequest(
    val modelId: ModelId,
    val text: String,
    val voiceName: String,
    val settings: SpeechSynthesisSettings,
)
