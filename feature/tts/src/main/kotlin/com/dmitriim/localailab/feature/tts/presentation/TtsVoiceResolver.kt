package com.dmitriim.localailab.feature.tts.presentation

import com.dmitriim.localailab.core.audio.input.storage.ReferenceVoice
import com.dmitriim.localailab.core.model.manifest.TtsVoiceMode

internal object TtsVoiceResolver {
    fun forModel(
        model: TtsModelOption?,
        language: TtsLanguage,
        references: List<ReferenceVoice>,
    ): List<TtsVoiceOption> = if (model?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
        references.map { reference ->
            TtsVoiceOption(
                id = reference.id,
                displayName = reference.displayName,
                speakerId = null,
                languages = setOf("en"),
                description = "${reference.durationMs / 1_000.0} s · ${reference.sourceDescription}",
                reference = reference,
            )
        }
    } else {
        model?.compatibleVoices(language).orEmpty()
    }
}
