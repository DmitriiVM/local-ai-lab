package com.dmitriim.localailab.ai.sherpa.stt.profiles

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.sherpa.stt.SherpaSttSession
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts

interface SherpaSttProfile : ModelRuntimeProfile {
    fun open(
        request: SpeechToTextLoadRequest,
        artifacts: ModelArtifacts,
        threadCount: Int,
    ): SherpaSttSession
}