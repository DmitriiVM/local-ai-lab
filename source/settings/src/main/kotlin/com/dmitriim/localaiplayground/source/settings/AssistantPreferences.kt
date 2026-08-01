package com.dmitriim.localaiplayground.source.settings

import com.dmitriim.localaiplayground.core.model.engine.ComputePreference

data class AssistantPreferences(
    val chat: AssistantChatPreferences = AssistantChatPreferences(),
    val speechInput: AssistantSpeechInputPreferences = AssistantSpeechInputPreferences(),
    val speechOutput: AssistantSpeechOutputPreferences = AssistantSpeechOutputPreferences(),
)

data class AssistantChatPreferences(
    val modelId: String? = null,
    val computePreference: ComputePreference = ComputePreference.CPU,
    val systemPrompt: String = "You are a helpful, concise assistant.",
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val maxOutputTokens: Int = 128,
    val seed: Int? = null,
    val contextSize: Int = 512,
    val threadCount: Int = 0,
)

data class AssistantSpeechInputPreferences(
    val modelId: String? = null,
    val languageCode: String = "en",
    val threadCount: Int = 0,
)

data class AssistantSpeechOutputPreferences(
    val modelId: String? = null,
    val voiceId: String? = null,
    val languageCode: String = "en",
    val speed: Float = 1f,
    val volume: Float = 1f,
    val sentenceSilenceScale: Float = 1f,
    val threadCount: Int = 0,
)
