package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import dev.zacsweers.metro.Inject

@Inject
class MoonshineV1SttProfile :
    BaseSherpaSttProfile(
        ModelProfileIds.MOONSHINE_V1_STT,
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) = offlineSherpaSession(artifacts, threadCount) {
        moonshine = OfflineMoonshineModelConfig().apply {
            preprocessor = artifacts.require(ModelFileRoles.PREPROCESSOR).path
            encoder = artifacts.require(ModelFileRoles.ENCODER).path
            uncachedDecoder = artifacts.require(ModelFileRoles.UNCACHED_DECODER).path
            cachedDecoder = artifacts.require(ModelFileRoles.CACHED_DECODER).path
        }
    }
}
