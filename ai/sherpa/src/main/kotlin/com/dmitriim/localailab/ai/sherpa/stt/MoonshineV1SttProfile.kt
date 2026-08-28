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
        "Moonshine v1 STT bundle",
        sherpaSttImport(
            "Moonshine v1 STT bundle",
            ModelFileRoles.PREPROCESSOR to "preprocess.onnx",
            ModelFileRoles.ENCODER to "encode.int8.onnx",
            ModelFileRoles.UNCACHED_DECODER to "uncached_decode.int8.onnx",
            ModelFileRoles.CACHED_DECODER to "cached_decode.int8.onnx",
            ModelFileRoles.TOKENS to "tokens.txt",
        ),
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
