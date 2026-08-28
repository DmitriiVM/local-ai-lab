package com.dmitriim.localailab.ai.api.system

import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey

/** Runtime profile keys for models provided by Android rather than installed model artifacts. */
object SystemModelProfileKeys {
    val ANDROID_SPEECH_RECOGNIZER = ModelProfileKey(
        engineId = EngineId("android-speech-recognizer"),
        profileId = ModelProfileId("ANDROID_SPEECH_RECOGNIZER_STT"),
    )
    val ANDROID_TEXT_TO_SPEECH = ModelProfileKey(
        engineId = EngineId("android-text-to-speech"),
        profileId = ModelProfileId("ANDROID_TEXT_TO_SPEECH_TTS"),
    )
}
