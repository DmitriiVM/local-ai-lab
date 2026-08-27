package com.dmitriim.localailab.ai.sherpa.tts

import com.k2fsa.sherpa.onnx.OfflineTts

data class SherpaTtsModel(
    val runtime: OfflineTts,
    val referenceAudio: Pcm16Wave? = null,
)
