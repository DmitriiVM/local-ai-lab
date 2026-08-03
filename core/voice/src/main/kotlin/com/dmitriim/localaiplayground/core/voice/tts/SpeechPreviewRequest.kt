package com.dmitriim.localaiplayground.core.voice.tts

import com.dmitriim.localaiplayground.core.model.manifest.ModelId

data class SpeechPreviewRequest(
    val modelId: ModelId,
    val text: String,
    val voiceName: String,
    val settings: SpeechSynthesisSettings,
)
