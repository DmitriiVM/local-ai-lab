package com.dmitriim.localailab.ai.sherpa.tts.profiles

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.ai.sherpa.tts.SherpaTtsModel
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.GenerationConfig

interface SherpaTtsProfile : ModelRuntimeProfile {
    fun open(artifacts: ModelArtifacts, threadCount: Int): SherpaTtsModel

    fun configureGeneration(
        config: GenerationConfig,
        request: TextToSpeechRequest,
        model: SherpaTtsModel,
    ) {
        config.extra = mapOf("lang" to request.languageCode)
    }
}