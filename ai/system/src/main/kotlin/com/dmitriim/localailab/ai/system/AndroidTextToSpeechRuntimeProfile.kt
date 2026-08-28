package com.dmitriim.localailab.ai.system

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.File

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelRuntimeProfile>())
class AndroidTextToSpeechRuntimeProfile : ModelRuntimeProfile {
    override val key = ModelProfileKey(
        EngineId("android-text-to-speech"),
        ModelProfileIds.ANDROID_TEXT_TO_SPEECH_TTS,
    )
    override fun validate(manifest: ModelManifest, directory: File) = RuntimeValidationResult(false, "Android text to speech is system managed.")
}
