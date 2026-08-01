package com.dmitriim.localaiplayground.feature.voice.domain

import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceConversationTurn

data class VoiceTurnRequest(
    val speechModelId: ModelId,
    val chatModelId: ModelId,
    val voiceModelId: ModelId,
    val languageCode: String,
    val systemPrompt: String,
    val temperature: Float,
    val maxOutputTokens: Int,
    val contextSize: Int,
    val sttThreadCount: Int,
    val llmThreadCount: Int,
    val ttsThreadCount: Int,
    val speakerId: Int,
    val speechRate: Float,
    val volume: Float,
    val history: List<VoiceConversationTurn>,
) {
    fun validate() {
        require(languageCode in setOf("en", "ru")) { "Select a supported conversation language." }
        require(temperature in 0f..2f) { "Temperature must be between 0 and 2." }
        require(contextSize in 128..32_768) { "Context size must be between 128 and 32,768 tokens." }
        require(maxOutputTokens in 1 until contextSize) { "Maximum output must be positive and smaller than context size." }
        require(sttThreadCount in 0..64 && llmThreadCount in 0..64 && ttsThreadCount in 0..64) {
            "Thread counts must be between 0 and 64."
        }
        require(speakerId >= 0) { "Speaker ID cannot be negative." }
        require(speechRate in 0.5f..2f) { "Speech rate must be between 0.5 and 2.0." }
        require(volume in 0f..1f) { "Playback volume must be between 0 and 1." }
    }
}
