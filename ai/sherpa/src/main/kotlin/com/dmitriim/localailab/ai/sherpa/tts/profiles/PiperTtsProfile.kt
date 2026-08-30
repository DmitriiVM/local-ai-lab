package com.dmitriim.localailab.ai.sherpa.tts.profiles

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRole
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import dev.zacsweers.metro.Inject

internal object PiperTtsArtifacts {
    val MODEL = ModelFileRole("VITS_MODEL")
}

private val piperTtsProfileId = ModelProfileId("PIPER_VITS_TTS")

@Inject
class PiperTtsProfile :
    BaseSherpaTtsProfile(
        piperTtsProfileId,
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int) = openSherpaTts(threadCount) {
        vits = OfflineTtsVitsModelConfig().apply {
            model = artifacts.require(PiperTtsArtifacts.MODEL).path
            tokens = artifacts.require(ModelFileRoles.TOKENS).path
            dataDir = artifacts.require(ModelFileRoles.FRONTEND_DATA).path
        }
    }
}
