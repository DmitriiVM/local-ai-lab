package com.dmitriim.localailab.core.model.library

import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelProfileId

data class ModelImportRequest(
    val displayName: String,
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val documentUris: List<String>,
    val directoryUri: String? = null,
)
