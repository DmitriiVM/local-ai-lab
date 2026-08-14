package com.dmitriim.localailab.core.voice.tts

import com.dmitriim.localailab.core.model.manifest.ModelId

data class SpeechPreviewRequest(
    val modelId: ModelId,
    val text: String,
    val voiceName: String,
    val settings: SpeechSynthesisSettings,
)
