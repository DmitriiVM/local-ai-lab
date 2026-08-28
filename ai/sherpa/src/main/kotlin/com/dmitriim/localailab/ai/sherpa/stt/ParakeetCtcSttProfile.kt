package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import dev.zacsweers.metro.Inject

private val parakeetCtcSttProfileId = ModelProfileId("PARAKEET_CTC_STT")

@Inject
class ParakeetCtcSttProfile :
    BaseSherpaSttProfile(
        parakeetCtcSttProfileId,
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) = nemoCtcSession(artifacts, threadCount)
}
