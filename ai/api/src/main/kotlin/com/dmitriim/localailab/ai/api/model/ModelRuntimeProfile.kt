package com.dmitriim.localailab.ai.api.model

import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
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

    /** Validates that [manifest] and its installed [directory] satisfy this profile's requirements. */
    fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult
}
