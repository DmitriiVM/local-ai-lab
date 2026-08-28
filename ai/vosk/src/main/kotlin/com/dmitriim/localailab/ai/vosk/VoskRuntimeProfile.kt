package com.dmitriim.localailab.ai.vosk

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import dev.zacsweers.metro.Inject
import java.io.File

internal interface VoskProfile : ModelRuntimeProfile

@Inject
class VoskRuntimeProfile : VoskProfile {
    override val key = ModelProfileKey(EngineId("vosk"), ModelProfileIds.VOSK_STT)
    override val capabilities = setOf(AiCapability.SPEECH_TO_TEXT)
    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.engineId == key.engineId && manifest.profileType == key.profileId)
        listOf("am", "conf", "graph").forEach { name ->
            require(File(directory, name).isDirectory) { "Missing Vosk $name directory." }
        }
    }.fold(
        onSuccess = { RuntimeValidationResult(true) },
        onFailure = { RuntimeValidationResult(false, it.message) },
    )
}
