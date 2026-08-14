package com.dmitriim.localailab.source.models.catalog.tts

import com.dmitriim.localailab.core.model.manifest.TtsVoiceDescriptor

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
