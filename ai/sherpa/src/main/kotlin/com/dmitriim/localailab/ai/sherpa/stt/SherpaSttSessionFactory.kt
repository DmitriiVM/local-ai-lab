package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig

internal fun offlineSherpaSession(
    artifacts: ModelArtifacts,
    threadCount: Int,
    configure: OfflineModelConfig.() -> Unit,
): SherpaSttSession = SherpaSttSession.Offline(
    OfflineRecognizer(
        null,
        OfflineRecognizerConfig().apply {
            modelConfig = OfflineModelConfig().apply {
                tokens = artifacts.require(ModelFileRoles.TOKENS).path
                numThreads = threadCount
                provider = "cpu"
                debug = false
                configure()
            }
        },
    ),
)
