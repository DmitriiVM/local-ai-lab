package com.dmitriim.localailab.ai.sherpa.stt.profiles

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.sherpa.stt.offlineSherpaSession
import com.k2fsa.sherpa.onnx.OfflineParaformerModelConfig
import dev.zacsweers.metro.Inject

private val paraformerSttProfileId = ModelProfileId("PARAFORMER_STT")

@Inject
class ParaformerSttProfile :
    BaseSherpaSttProfile(
        paraformerSttProfileId,
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) =
        offlineSherpaSession(artifacts, threadCount) {
            paraformer = OfflineParaformerModelConfig().apply {
                model = artifacts.require(ModelFileRoles.PRIMARY_MODEL).path
            }
        }
}
