package com.dmitriim.localailab.ai.system

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.system.SystemModelProfileKeys
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.File

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelRuntimeProfile>())
class AndroidSpeechRecognizerRuntimeProfile : ModelRuntimeProfile {
    override val key = SystemModelProfileKeys.ANDROID_SPEECH_RECOGNIZER

    override fun validate(
        manifest: ModelManifest,
        directory: File,
    ) = RuntimeValidationResult(
        valid = false,
        message = "Android speech recognition is system managed.",
    )
}
