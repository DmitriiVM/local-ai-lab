package com.dmitriim.localailab.feature.assistant.impl.presentation.state

import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelId

data class SpeechModelOption(
    val id: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val languages: Set<String>,
    val sampleRateHz: Int,
    val installed: Boolean,
) {
    fun supports(languageCode: String): Boolean = languages.isEmpty() ||
        languages.any {
            normalizeLanguageCode(it) == languageCode
        }
}
