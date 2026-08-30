package com.dmitriim.localailab.feature.assistant.impl.presentation.state

internal data class SpeechOutcome(
    val succeeded: Boolean = true,
    val error: String? = null,
)
