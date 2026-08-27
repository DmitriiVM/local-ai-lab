package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig

internal fun singleModelCtcImport(displayName: String) = sherpaSttImport(
    displayName,
    ModelFileRoles.PRIMARY_MODEL to "model.int8.onnx",
    ModelFileRoles.TOKENS to "tokens.txt",
)

internal fun nemoCtcSession(artifacts: ModelArtifacts, threadCount: Int) = offlineSherpaSession(artifacts, threadCount) {
    nemo = OfflineNemoEncDecCtcModelConfig().apply {
        model = artifacts.require(ModelFileRoles.PRIMARY_MODEL).path
    }
}
