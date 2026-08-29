package com.dmitriim.localailab.ai.sherpa.catalog.tts

import com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceDescriptor

internal fun ttsVoice(
    id: String,
    displayName: String,
    speakerId: Int,
    description: String? = null,
    vararg languages: String,
) = TtsVoiceDescriptor(
    id = id,
    displayName = displayName,
    speakerId = speakerId,
    languages = languages.toSet(),
    description = description,
)
