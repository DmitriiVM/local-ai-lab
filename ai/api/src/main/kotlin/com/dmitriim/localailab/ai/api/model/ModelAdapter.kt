package com.dmitriim.localailab.ai.api.model

import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import java.io.File

/** A packaged model-family integration. Model weights may be imported or downloaded separately. */
interface ModelAdapter {
    val id: String
    val profileTypes: Set<ModelProfileId>
    val engineId: EngineId
    val capabilities: Set<AiCapability>

    fun capabilitiesFor(profileType: ModelProfileId): Set<AiCapability>
    fun importDefinition(profileType: ModelProfileId): ModelImportDefinition?
    fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult
}
