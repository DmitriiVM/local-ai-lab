package com.dmitriim.localailab.ai.sherpa.tts.profiles

import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.ai.sherpa.tts.SherpaTtsModel
import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsPocketModelConfig
import dev.zacsweers.metro.Inject
import java.io.File

internal object PocketTtsArtifacts {
    val LM_FLOW = ModelFileRole("LM_FLOW")
    val LM_MAIN = ModelFileRole("LM_MAIN")
    val ENCODER = ModelFileRole("POCKET_ENCODER")
    val DECODER = ModelFileRole("POCKET_DECODER")
    val TEXT_CONDITIONER = ModelFileRole("TEXT_CONDITIONER")
    val VOCABULARY = ModelFileRole("VOCABULARY")
    val TOKEN_SCORES = ModelFileRole("TOKEN_SCORES")
    val REFERENCE_AUDIO = ModelFileRole("REFERENCE_AUDIO")
}

private val pocketTtsProfileId = ModelProfileId("POCKET_TTS")

@Inject
class PocketTtsProfile :
    BaseSherpaTtsProfile(
        pocketTtsProfileId,
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int): SherpaTtsModel {
        val runtime = OfflineTts(
            null,
            OfflineTtsConfig().apply {
                model = OfflineTtsModelConfig().apply {
                    pocket = OfflineTtsPocketModelConfig().apply {
                        lmFlow = artifacts.require(PocketTtsArtifacts.LM_FLOW).path
                        lmMain = artifacts.require(PocketTtsArtifacts.LM_MAIN).path
                        encoder = artifacts.require(PocketTtsArtifacts.ENCODER).path
                        decoder = artifacts.require(PocketTtsArtifacts.DECODER).path
                        textConditioner = artifacts.require(PocketTtsArtifacts.TEXT_CONDITIONER).path
                        vocabJson = artifacts.require(PocketTtsArtifacts.VOCABULARY).path
                        tokenScoresJson = artifacts.require(PocketTtsArtifacts.TOKEN_SCORES).path
                        voiceEmbeddingCacheCapacity = 1
                    }
                    numThreads = threadCount
                    provider = "cpu"
                    debug = false
                }
            },
        )
        return SherpaTtsModel(
            runtime = runtime,
            referenceAudio = Pcm16Wave.read(
                File(artifacts.require(PocketTtsArtifacts.REFERENCE_AUDIO).path),
            ),
        )
    }

    override fun configureGeneration(
        config: GenerationConfig,
        request: TextToSpeechRequest,
        model: SherpaTtsModel,
    ) {
        val reference = requireNotNull(model.referenceAudio) {
            "Pocket TTS requires the bundled default reference voice."
        }
        config.referenceAudio = reference.samples
        config.referenceSampleRate = reference.sampleRateHz
        config.numSteps = 5
        config.extra = mapOf("max_reference_audio_len" to "12", "seed" to "42")
    }
}
