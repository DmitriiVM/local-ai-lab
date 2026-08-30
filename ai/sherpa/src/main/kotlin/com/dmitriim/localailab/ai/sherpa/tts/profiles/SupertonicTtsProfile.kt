package com.dmitriim.localailab.ai.sherpa.tts.profiles

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRole
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import dev.zacsweers.metro.Inject

internal object SupertonicTtsArtifacts {
    val DURATION_PREDICTOR = ModelFileRole("DURATION_PREDICTOR")
    val TEXT_ENCODER = ModelFileRole("TEXT_ENCODER")
    val VECTOR_ESTIMATOR = ModelFileRole("VECTOR_ESTIMATOR")
    val UNICODE_INDEXER = ModelFileRole("UNICODE_INDEXER")
    val VOICE_STYLE = ModelFileRole("VOICE_STYLE")
}

private val supertonicTtsProfileId = ModelProfileId("SUPERTONIC_TTS")

@Inject
class SupertonicTtsProfile :
    BaseSherpaTtsProfile(
        supertonicTtsProfileId,
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int) = openSherpaTts(threadCount) {
        supertonic = OfflineTtsSupertonicModelConfig().apply {
            durationPredictor =
                artifacts.require(SupertonicTtsArtifacts.DURATION_PREDICTOR).path
            textEncoder = artifacts.require(SupertonicTtsArtifacts.TEXT_ENCODER).path
            vectorEstimator = artifacts.require(SupertonicTtsArtifacts.VECTOR_ESTIMATOR).path
            vocoder = artifacts.require(ModelFileRoles.VOCODER).path
            ttsJson = artifacts.require(ModelFileRoles.CONFIG).path
            unicodeIndexer = artifacts.require(SupertonicTtsArtifacts.UNICODE_INDEXER).path
            voiceStyle = artifacts.require(SupertonicTtsArtifacts.VOICE_STYLE).path
        }
    }
}
