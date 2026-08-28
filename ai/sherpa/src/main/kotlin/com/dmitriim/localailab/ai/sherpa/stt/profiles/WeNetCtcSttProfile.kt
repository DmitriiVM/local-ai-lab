package com.dmitriim.localailab.ai.sherpa.stt.profiles

import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.sherpa.stt.offlineSherpaSession
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineWenetCtcModelConfig
import dev.zacsweers.metro.Inject

private val weNetCtcSttProfileId = ModelProfileId("WENET_CTC_STT")

/** Runtime profile for offline WeNet CTC speech-to-text bundles. */
@Inject
class WeNetCtcSttProfile :
    BaseSherpaSttProfile(
        weNetCtcSttProfileId,
    ) {
    override fun open(
        request: SpeechToTextLoadRequest,
        artifacts: ModelArtifacts,
        threadCount: Int,
    ) = offlineSherpaSession(artifacts, threadCount) {
        wenetCtc = OfflineWenetCtcModelConfig().apply {
            model = artifacts.require(ModelFileRoles.PRIMARY_MODEL).path
        }
    }
}
