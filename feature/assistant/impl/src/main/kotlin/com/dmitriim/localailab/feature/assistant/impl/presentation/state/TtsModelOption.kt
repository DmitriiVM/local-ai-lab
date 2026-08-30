package com.dmitriim.localailab.feature.assistant.impl.presentation.state

import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.manifest.TtsControl
import com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceMode
import com.dmitriim.localailab.core.audio.input.storage.ReferenceVoice

data class TtsModelOption(
    val id: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val languages: Set<String>,
    val speakerCount: Int?,
    val voiceMode: TtsVoiceMode,
    val supportedControls: Set<TtsControl>,
    val voices: List<TtsVoiceOption>,
    val installed: Boolean,
) {
    fun compatibleVoices(languageCode: String): List<TtsVoiceOption> {
        if (languages.isNotEmpty() && languages.none { normalizeLanguageCode(it) == languageCode }) return emptyList()
        return voices.filter { voice ->
            voice.languages.isEmpty() || voice.languages.any { normalizeLanguageCode(it) == languageCode }
        }
    }
}

data class TtsVoiceOption(
    val id: String,
    val displayName: String,
    val speakerId: Int?,
    val languages: Set<String>,
    val description: String?,
    val reference: ReferenceVoice? = null,
    val platformVoiceId: String? = null,
)
