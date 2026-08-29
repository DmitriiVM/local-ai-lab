package com.dmitriim.localailab.ai.sherpa.tts.profiles

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRole
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig
import dev.zacsweers.metro.Inject

private val kittenTtsProfileId = ModelProfileId("KITTEN_TTS")

/** Semantic artifact roles shared by the Kitten catalog entry and runtime profile. */
internal object KittenTtsArtifacts {
    val MODEL = ModelFileRole("KITTEN_MODEL")
    val VOICES = ModelFileRole("KITTEN_VOICES")
}

/** Runtime contract for the Sherpa-ONNX KittenTTS model bundle. */
@Inject
class KittenTtsProfile :
    BaseSherpaTtsProfile(
        kittenTtsProfileId,
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int) =
        openSherpaTts(threadCount) {
            kitten = OfflineTtsKittenModelConfig().apply {
                model = artifacts.require(KittenTtsArtifacts.MODEL).path
                voices = artifacts.require(KittenTtsArtifacts.VOICES).path
                tokens = artifacts.require(ModelFileRoles.TOKENS).path
                dataDir = artifacts.require(ModelFileRoles.FRONTEND_DATA).path
            }
        }
}
