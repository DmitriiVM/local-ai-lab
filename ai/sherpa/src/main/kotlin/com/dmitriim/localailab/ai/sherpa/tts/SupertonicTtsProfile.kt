package com.dmitriim.localailab.ai.sherpa.tts

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import dev.zacsweers.metro.Inject

@Inject
class SupertonicTtsProfile :
    BaseSherpaTtsProfile(
        ModelProfileIds.SUPERTONIC_TTS,
    ) {
    override fun open(artifacts: ModelArtifacts, threadCount: Int) = openSherpaTts(threadCount) {
        supertonic = OfflineTtsSupertonicModelConfig().apply {
            durationPredictor = artifacts.require(ModelFileRoles.DURATION_PREDICTOR).path
            textEncoder = artifacts.require(ModelFileRoles.TEXT_ENCODER).path
            vectorEstimator = artifacts.require(ModelFileRoles.VECTOR_ESTIMATOR).path
            vocoder = artifacts.require(ModelFileRoles.VOCODER).path
            ttsJson = artifacts.require(ModelFileRoles.CONFIG).path
            unicodeIndexer = artifacts.require(ModelFileRoles.UNICODE_INDEXER).path
            voiceStyle = artifacts.require(ModelFileRoles.VOICE_STYLE).path
        }
    }
}
