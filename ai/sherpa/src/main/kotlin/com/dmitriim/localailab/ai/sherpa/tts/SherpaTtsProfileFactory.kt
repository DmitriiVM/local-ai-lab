package com.dmitriim.localailab.ai.sherpa.tts

import com.dmitriim.localailab.ai.api.model.ModelImportDefinition
import com.dmitriim.localailab.ai.api.model.ModelImportFileDefinition
import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig

internal fun sherpaTtsImport(
    displayName: String,
    vararg files: Triple<ModelFileRole, String, Boolean>,
) = ModelImportDefinition(
    displayName = displayName,
    format = ModelFormat.ONNX,
    files = files.map { (role, path, directory) ->
        ModelImportFileDefinition(role, relativePath = path, directory = directory)
    },
)

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

internal infix fun Pair<ModelFileRole, String>.directory(directory: Boolean) = Triple(first, second, directory)

internal fun Pair<ModelFileRole, String>.file() = Triple(first, second, false)
