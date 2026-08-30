package com.dmitriim.localailab.ai.sherpa.stt.profiles

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.sherpa.stt.SherpaSttSession
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import dev.zacsweers.metro.Inject

private val zipformerSttProfileId = ModelProfileId("ZIPFORMER_STT")

@Inject
class ZipformerSttProfile :
    BaseSherpaSttProfile(
        zipformerSttProfileId,
    ) {
    override fun open(
        request: SpeechToTextLoadRequest,
        artifacts: ModelArtifacts,
        threadCount: Int,
    ) = SherpaSttSession.Streaming(
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
