package com.dmitriim.localailab.feature.assistant.impl.presentation.state

data class SpeechInputSettings(
    val languageCode: String = "en",
    val threadCount: String = "0",
) {
    fun validate() {
        require(languageCode.isNotBlank()) { "Select a recognition language." }
        require(threadCount.toIntOrNull() in 0..64) { "Thread count must be between 0 and 64." }
    }
}
