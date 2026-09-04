package com.dmitriim.localailab.ai.sherpa.tts.profiles

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRole
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig
import dev.zacsweers.metro.Inject

internal object MatchaTtsArtifacts {
    val ACOUSTIC_MODEL = ModelFileRole("MATCHA_ACOUSTIC_MODEL")
}

private val matchaTtsProfileId = ModelProfileId("MATCHA_TTS")

/** Runtime contract for a Matcha-TTS acoustic model paired with a Vocos vocoder. */
@Inject
class MatchaTtsProfile :
    BaseSherpaTtsProfile(
        matchaTtsProfileId,
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int) = openSherpaTts(threadCount) {
        matcha = OfflineTtsMatchaModelConfig().apply {
            acousticModel = artifacts.require(MatchaTtsArtifacts.ACOUSTIC_MODEL).path
            vocoder = artifacts.require(ModelFileRoles.VOCODER).path
            tokens = artifacts.require(ModelFileRoles.TOKENS).path
            dataDir = artifacts.require(ModelFileRoles.FRONTEND_DATA).path
        }
    }
}
