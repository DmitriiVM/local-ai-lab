package com.dmitriim.localaiplayground.ai.api

import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelFileRole
import com.dmitriim.localaiplayground.core.model.ModelFormat
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelProfileId
import dev.zacsweers.metro.Inject
import java.io.File

data class RuntimeValidationResult(
    val valid: Boolean,
    val message: String? = null,
)

/** Adapter-owned rules for a user-supplied model bundle. */
data class ModelImportDefinition(
    val displayName: String,
    val format: ModelFormat,
    val files: List<ModelImportFileDefinition>,
)

data class ModelImportFileDefinition(
    val role: ModelFileRole,
    val relativePath: String? = null,
    val extension: String? = null,
    val directory: Boolean = false,
) {
    init {
        require((relativePath == null) != (extension == null)) { "An import file must declare one path or extension." }
    }
}

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

/** Resolves one adapter per persisted profile ID and rejects ambiguous app packaging. */
@Inject
class ModelAdapterRegistry(adapters: Set<ModelAdapter>) {
    init {
        require(adapters.map { it.id }.distinct().size == adapters.size) {
            "More than one packaged model adapter declares the same adapter ID."
        }
    }

    private val byProfile = buildMap {
        adapters.forEach { adapter ->
            adapter.profileTypes.forEach { profileType ->
                require(put(profileType, adapter) == null) {
                    "More than one packaged model adapter declares ${profileType.value}."
                }
            }
        }
    }

    fun find(profileType: ModelProfileId): ModelAdapter? = byProfile[profileType]
}
