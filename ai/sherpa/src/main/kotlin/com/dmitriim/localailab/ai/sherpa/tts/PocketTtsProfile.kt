package com.dmitriim.localailab.ai.sherpa.tts

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsPocketModelConfig
import dev.zacsweers.metro.Inject
import java.io.File

@Inject
class PocketTtsProfile :
    BaseSherpaTtsProfile(
        ModelProfileIds.POCKET_TTS,
        "Pocket TTS INT8 (English)",
        sherpaTtsImport(
            "Pocket TTS INT8 (English)",
            (ModelFileRoles.LM_FLOW to "lm_flow.int8.onnx").file(),
            (ModelFileRoles.LM_MAIN to "lm_main.int8.onnx").file(),
            (ModelFileRoles.POCKET_ENCODER to "encoder.onnx").file(),
            (ModelFileRoles.POCKET_DECODER to "decoder.int8.onnx").file(),
            (ModelFileRoles.TEXT_CONDITIONER to "text_conditioner.onnx").file(),
            (ModelFileRoles.VOCABULARY to "vocab.json").file(),
            (ModelFileRoles.TOKEN_SCORES to "token_scores.json").file(),
            (ModelFileRoles.REFERENCE_AUDIO to "test_wavs/bria.wav").file(),
        ),
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int): SherpaTtsModel {
        val runtime = OfflineTts(
            null,
            OfflineTtsConfig().apply {
                model = OfflineTtsModelConfig().apply {
                    pocket = OfflineTtsPocketModelConfig().apply {
                        lmFlow = artifacts.require(ModelFileRoles.LM_FLOW).path
                        lmMain = artifacts.require(ModelFileRoles.LM_MAIN).path
                        encoder = artifacts.require(ModelFileRoles.POCKET_ENCODER).path
                        decoder = artifacts.require(ModelFileRoles.POCKET_DECODER).path
                        textConditioner = artifacts.require(ModelFileRoles.TEXT_CONDITIONER).path
                        vocabJson = artifacts.require(ModelFileRoles.VOCABULARY).path
                        tokenScoresJson = artifacts.require(ModelFileRoles.TOKEN_SCORES).path
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
            referenceAudio = Pcm16Wave.read(File(artifacts.require(ModelFileRoles.REFERENCE_AUDIO).path)),
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
