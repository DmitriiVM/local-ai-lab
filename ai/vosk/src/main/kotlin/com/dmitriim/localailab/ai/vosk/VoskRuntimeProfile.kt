package com.dmitriim.localailab.ai.vosk

import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import dev.zacsweers.metro.Inject
import java.io.File

internal interface VoskProfile : ModelRuntimeProfile

private val voskSttProfileId = ModelProfileId("VOSK_STT")

@Inject
class VoskRuntimeProfile : VoskProfile {
    override val key = ModelProfileKey(EngineId("vosk"), voskSttProfileId)
    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        listOf("am", "conf", "graph").forEach { name ->
            require(File(directory, name).isDirectory) { "Missing Vosk $name directory." }
        }
    }.fold(
        onSuccess = { RuntimeValidationResult(true) },
        onFailure = { RuntimeValidationResult(false, it.message) },
    )
}
