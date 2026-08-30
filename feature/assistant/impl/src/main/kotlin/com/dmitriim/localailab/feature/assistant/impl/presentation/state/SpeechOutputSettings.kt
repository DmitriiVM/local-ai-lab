package com.dmitriim.localailab.feature.assistant.impl.presentation.state

data class SpeechOutputSettings(
    val languageCode: String = "en",
    val speed: String = "1.0",
    val volume: String = "1.0",
    val sentenceSilenceScale: String = "1.0",
    val threadCount: String = "0",
) {
    fun validate() {
        val speedValue = speed.toFloatOrNull() ?: error("Speech rate must be a number.")
        val volumeValue = volume.toFloatOrNull() ?: error("Volume must be a number.")
        val silenceValue = sentenceSilenceScale.toFloatOrNull() ?: error("Sentence silence must be a number.")
        val threadValue = threadCount.toIntOrNull() ?: error("Thread count must be a whole number.")
        require(languageCode in setOf("en", "ru", "zh")) { "Select a supported speech language." }
        require(speedValue in 0.5f..2f) { "Speech rate must be between 0.5 and 2.0." }
        require(volumeValue in 0f..1f) { "Volume must be between 0 and 1." }
        require(silenceValue in 0f..2f) { "Sentence silence must be between 0 and 2." }
        require(threadValue in 0..64) { "Thread count must be between 0 and 64." }
    }
}
