package com.dmitriim.localailab.ai.sherpa.tts

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig

internal fun openSherpaTts(
    threadCount: Int,
    configure: OfflineTtsModelConfig.() -> Unit,
) = SherpaTtsModel(
    OfflineTts(
        null,
        OfflineTtsConfig().apply {
            model = OfflineTtsModelConfig().apply {
                configure()
                numThreads = threadCount
                provider = "cpu"
                debug = false
            }
        },
    ),
)
