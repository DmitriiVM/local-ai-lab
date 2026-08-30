package com.dmitriim.localailab.ai.sherpa.tts.profiles

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRole
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.dmitriim.localailab.ai.sherpa.tts.SherpaTtsModel
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import dev.zacsweers.metro.Inject

internal object KokoroTtsArtifacts {
    val MODEL = ModelFileRole("KOKORO_MODEL")
    val VOICES = ModelFileRole("VOICE_EMBEDDINGS")
    val LEXICON = ModelFileRole("LEXICON")
    val TEXT_RULES = ModelFileRole("TEXT_RULES")
    val DICTIONARY = ModelFileRole("DICTIONARY_DATA")
}

private val kokoroTtsProfileId = ModelProfileId("KOKORO_TTS")

@Inject
class KokoroTtsProfile :
    BaseSherpaTtsProfile(
        kokoroTtsProfileId,
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int): SherpaTtsModel = openSherpaTts(threadCount) {
        kokoro = OfflineTtsKokoroModelConfig().apply {
            model = artifacts.require(KokoroTtsArtifacts.MODEL).path
            voices = artifacts.require(KokoroTtsArtifacts.VOICES).path
            tokens = artifacts.require(ModelFileRoles.TOKENS).path
            dataDir = artifacts.require(ModelFileRoles.FRONTEND_DATA).path
            lexicon = listOf("lexicon-us-en.txt", "lexicon-zh.txt")
                .joinToString(",") { artifacts.requirePath(it).path }
            dictDir = artifacts.require(KokoroTtsArtifacts.DICTIONARY).path
        }
    }
}
