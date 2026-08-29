package com.dmitriim.localailab.ai.sherpa.stt.profiles

import com.dmitriim.localailab.ai.sherpa.stt.offlineSherpaSession
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig

internal fun nemoCtcSession(artifacts: ModelArtifacts, threadCount: Int) =
    offlineSherpaSession(artifacts, threadCount) {
        nemo = OfflineNemoEncDecCtcModelConfig().apply {
            model = artifacts.require(ModelFileRoles.PRIMARY_MODEL).path
        }
    }
