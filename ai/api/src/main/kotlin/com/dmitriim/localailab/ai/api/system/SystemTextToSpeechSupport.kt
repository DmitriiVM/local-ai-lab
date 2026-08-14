package com.dmitriim.localailab.ai.api.system

import kotlinx.coroutines.flow.StateFlow

/** Discovers on-device voices supplied by Android without coupling features to the platform module. */
interface SystemTextToSpeechSupport {
    val voices: StateFlow<List<SystemTextToSpeechVoice>>

    /** Initializes the platform engine and refreshes [voices]. This call may block. */
    fun refresh()
}
