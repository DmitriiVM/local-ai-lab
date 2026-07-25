package com.dmitriim.localaiplayground.ai.api

import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import java.io.File

/**
 * A loaded native object is intentionally represented only by its close handle.
 * Features use their later-stage engine APIs; Stage 2 owns coordinated lifetime.
 */
interface ModelRuntimeLoader {
    val engineId: EngineId

    fun load(manifest: ModelManifest, directory: File): AutoCloseable
}
