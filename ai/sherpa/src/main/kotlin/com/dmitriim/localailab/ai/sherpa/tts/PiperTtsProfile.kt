package com.dmitriim.localailab.ai.sherpa.tts

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import dev.zacsweers.metro.Inject

@Inject
class PiperTtsProfile :
    BaseSherpaTtsProfile(
        ModelProfileIds.PIPER_VITS_TTS,
        "Piper Lessac Medium (English)",
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int) = openSherpaTts(threadCount) {
        vits = OfflineTtsVitsModelConfig().apply {
            model = artifacts.require(ModelFileRoles.VITS_MODEL).path
            tokens = artifacts.require(ModelFileRoles.TOKENS).path
            dataDir = artifacts.require(ModelFileRoles.FRONTEND_DATA).path
        }
    }
}
