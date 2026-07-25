package com.dmitriim.localaiplayground.ai.llamacpp

import android.app.Application
import com.dmitriim.localaiplayground.ai.api.LlmLoadRequest
import com.dmitriim.localaiplayground.ai.api.ModelRuntimeLoader
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import java.io.File

@Inject
@ContributesIntoSet(AppScope::class)
class LlamaCppModelRuntimeLoader(
    private val application: Application,
) : ModelRuntimeLoader {
    override val engineId = EngineId("llama.cpp")

    override fun load(manifest: ModelManifest, directory: File): AutoCloseable {
        val model = manifest.files.first { it.required }
        return NativeLlama(application).also { engine ->
            engine.load(
                LlmLoadRequest(
                    modelPath = File(directory, model.relativePath).absolutePath,
                    contextSize = manifest.contextSize ?: 512,
                ),
            )
        }
    }
}
