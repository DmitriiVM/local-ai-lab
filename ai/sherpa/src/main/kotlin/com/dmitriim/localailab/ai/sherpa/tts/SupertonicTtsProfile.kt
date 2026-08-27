package com.dmitriim.localailab.ai.sherpa.tts

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelRuntimeProfile>())
class SupertonicTtsProfile :
    BaseSherpaTtsProfile(
        ModelProfileIds.SUPERTONIC_TTS,
        "Supertonic TTS bundle",
        sherpaTtsImport(
            "Supertonic TTS bundle",
            (ModelFileRoles.DURATION_PREDICTOR to "duration_predictor.int8.onnx").file(),
            (ModelFileRoles.TEXT_ENCODER to "text_encoder.int8.onnx").file(),
            (ModelFileRoles.VECTOR_ESTIMATOR to "vector_estimator.int8.onnx").file(),
            (ModelFileRoles.VOCODER to "vocoder.int8.onnx").file(),
            (ModelFileRoles.CONFIG to "tts.json").file(),
            (ModelFileRoles.UNICODE_INDEXER to "unicode_indexer.bin").file(),
            (ModelFileRoles.VOICE_STYLE to "voice.bin").file(),
        ),
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
