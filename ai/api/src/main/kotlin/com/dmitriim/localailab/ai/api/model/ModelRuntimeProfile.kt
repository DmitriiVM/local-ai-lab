package com.dmitriim.localailab.ai.api.model

import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import java.io.File

/**
 * Describes one model profile supported by a packaged runtime.
 *
 * The [key] binds persisted model metadata to this runtime contract independently from download
 * URLs or local file paths. [validate] is invoked after generic file/checksum validation and must
 * verify only profile-specific requirements. It must not load a model for inference.
 */
interface ModelRuntimeProfile {
    /** Unique engine/profile key accepted by this runtime. */
    val key: ModelProfileKey
    /** Human-readable label used when presenting the profile to a user. */
    val displayName: String
    /** Capabilities granted to models using this profile. */
    val capabilities: Set<AiCapability>
    /** Validates that [manifest] and its installed [directory] satisfy this profile's requirements. */
    fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult
}
