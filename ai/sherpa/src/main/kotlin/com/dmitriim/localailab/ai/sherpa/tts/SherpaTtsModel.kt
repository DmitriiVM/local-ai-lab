package com.dmitriim.localailab.ai.sherpa.tts

import com.dmitriim.localailab.ai.sherpa.tts.profiles.Pcm16Wave
import com.k2fsa.sherpa.onnx.OfflineTts

data class SherpaTtsModel(
    val runtime: OfflineTts,
    val referenceAudio: Pcm16Wave? = null,
)
