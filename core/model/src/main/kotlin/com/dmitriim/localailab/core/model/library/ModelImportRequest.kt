package com.dmitriim.localailab.core.model.library

import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey

data class ModelImportRequest(
    val displayName: String,
    val profileKey: ModelProfileKey,
    val documentUris: List<String>,
    val directoryUri: String? = null,
) {
    /** Compatibility constructor for callers that have not adopted [ModelProfileKey] yet. */
    constructor(
        displayName: String,
        engineId: EngineId,
        profileType: ModelProfileId,
        documentUris: List<String>,
        directoryUri: String? = null,
    ) : this(
        displayName = displayName,
        profileKey = ModelProfileKey(engineId, profileType),
        documentUris = documentUris,
        directoryUri = directoryUri,
    )

    val engineId: EngineId
        get() = profileKey.engineId

    val profileType: ModelProfileId
        get() = profileKey.profileId
}
