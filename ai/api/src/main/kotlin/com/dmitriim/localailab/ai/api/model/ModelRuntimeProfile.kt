package com.dmitriim.localailab.ai.api.model

import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import java.io.File

/** One reusable runtime integration, keyed independently from downloadable model artifacts. */
interface ModelRuntimeProfile {
    val key: ModelProfileKey
    val displayName: String
    val capabilities: Set<AiCapability>
    val importDefinition: ModelImportDefinition?

    fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult
}
