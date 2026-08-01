package com.dmitriim.localaiplayground.ai.api.system

/** A locally available voice exposed by the operating system's text-to-speech service. */
data class SystemTextToSpeechVoice(
    val id: String,
    val displayName: String,
    val languageTag: String,
    val description: String?,
)
