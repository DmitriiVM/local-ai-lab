package com.dmitriim.localailab.feature.assistant.impl.presentation.state

internal data class GenerationOutcome(
    val text: String,
    val speechSucceeded: Boolean,
    val speechError: String?,
)
