package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import dev.zacsweers.metro.Inject

@Inject
class MoonshineV2SttProfile :
    BaseSherpaSttProfile(
        ModelProfileIds.MOONSHINE_STT,
        "Moonshine STT bundle",
        sherpaSttImport(
            "Moonshine STT bundle",
            ModelFileRoles.ENCODER to "encoder_model.ort",
            ModelFileRoles.MERGED_DECODER to "decoder_model_merged.ort",
            ModelFileRoles.TOKENS to "tokens.txt",
        ),
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) = offlineSherpaSession(artifacts, threadCount) {
        moonshine = OfflineMoonshineModelConfig().apply {
            encoder = artifacts.require(ModelFileRoles.ENCODER).path
            mergedDecoder = artifacts.require(ModelFileRoles.MERGED_DECODER).path
        }
    }
}
