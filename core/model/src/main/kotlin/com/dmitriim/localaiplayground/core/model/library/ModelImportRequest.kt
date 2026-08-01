package com.dmitriim.localaiplayground.core.model.library

import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId

data class ModelImportRequest(
    val displayName: String,
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val documentUris: List<String>,
    val directoryUri: String? = null,
)
