package com.dmitriim.localailab.ai.sherpa.tts

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import dev.zacsweers.metro.Inject

@Inject
class KokoroTtsProfile :
    BaseSherpaTtsProfile(
        ModelProfileIds.KOKORO_TTS,
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int): SherpaTtsModel = openSherpaTts(threadCount) {
        kokoro = OfflineTtsKokoroModelConfig().apply {
            model = artifacts.require(ModelFileRoles.KOKORO_MODEL).path
            voices = artifacts.require(ModelFileRoles.VOICE_EMBEDDINGS).path
            tokens = artifacts.require(ModelFileRoles.TOKENS).path
            dataDir = artifacts.require(ModelFileRoles.FRONTEND_DATA).path
            lexicon = listOf("lexicon-us-en.txt", "lexicon-zh.txt")
                .joinToString(",") { artifacts.requirePath(it).path }
            dictDir = artifacts.require(ModelFileRoles.DICTIONARY_DATA).path
        }
    }
}
