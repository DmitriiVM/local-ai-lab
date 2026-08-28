package com.dmitriim.localailab.ai.sherpa.stt.profiles

import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import dev.zacsweers.metro.Inject

private val gigaAmCtcSttProfileId = ModelProfileId("GIGAAM_CTC_STT")

@Inject
class GigaAmCtcSttProfile :
    BaseSherpaSttProfile(
        gigaAmCtcSttProfileId,
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) =
        nemoCtcSession(artifacts, threadCount)
}
