package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelRuntimeProfile>())
class ZipformerSttProfile :
    BaseSherpaSttProfile(
        ModelProfileIds.ZIPFORMER_STT,
        "Streaming Zipformer bundle",
        sherpaSttImport(
            "Streaming Zipformer bundle",
            ModelFileRoles.ENCODER to "encoder-epoch-99-avg-1.int8.onnx",
            ModelFileRoles.DECODER to "decoder-epoch-99-avg-1.int8.onnx",
            ModelFileRoles.JOINER to "joiner-epoch-99-avg-1.int8.onnx",
            ModelFileRoles.TOKENS to "tokens.txt",
        ),
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) = SherpaSttSession.Online(
        OnlineRecognizer(
            null,
            OnlineRecognizerConfig().apply {
                modelConfig = OnlineModelConfig().apply {
                    transducer = OnlineTransducerModelConfig().apply {
                        encoder = artifacts.require(ModelFileRoles.ENCODER).path
                        decoder = artifacts.require(ModelFileRoles.DECODER).path
                        joiner = artifacts.require(ModelFileRoles.JOINER).path
                    }
                    tokens = artifacts.require(ModelFileRoles.TOKENS).path
                    numThreads = threadCount
                    provider = "cpu"
                    debug = false
                }
                enableEndpoint = false
                decodingMethod = "greedy_search"
            },
        ),
    )
}
