package com.dmitriim.localailab.ai.sherpa.tts

import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
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
