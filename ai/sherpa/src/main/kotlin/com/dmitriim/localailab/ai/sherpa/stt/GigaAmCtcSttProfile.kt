package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import dev.zacsweers.metro.Inject

@Inject
class GigaAmCtcSttProfile :
    BaseSherpaSttProfile(
        ModelProfileIds.GIGAAM_CTC_STT,
        "GigaAM STT bundle",
        singleModelCtcImport("GigaAM STT bundle"),
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) = nemoCtcSession(artifacts, threadCount)
}
