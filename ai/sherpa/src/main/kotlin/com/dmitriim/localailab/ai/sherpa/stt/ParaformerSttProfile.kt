package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineParaformerModelConfig
import dev.zacsweers.metro.Inject

@Inject
class ParaformerSttProfile :
    BaseSherpaSttProfile(
        ModelProfileIds.PARAFORMER_STT,
        "Paraformer STT bundle",
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) = offlineSherpaSession(artifacts, threadCount) {
        paraformer = OfflineParaformerModelConfig().apply {
            model = artifacts.require(ModelFileRoles.PRIMARY_MODEL).path
        }
    }
}
