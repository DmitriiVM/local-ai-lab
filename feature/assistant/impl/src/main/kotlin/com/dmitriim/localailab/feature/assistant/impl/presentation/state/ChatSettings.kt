package com.dmitriim.localailab.feature.assistant.impl.presentation.state

import com.dmitriim.localailab.ai.api.engine.ComputePreference

data class ChatSettings(
    val computePreference: ComputePreference = ComputePreference.CPU,
    val systemPrompt: String = "You are a helpful, concise assistant.",
    val temperature: String = "0.7",
    val topK: String = "40",
    val topP: String = "0.9",
    val maxOutputTokens: String = "256",
    val seed: String = "",
    val contextSize: String = "2048",
    val threadCount: String = "0",
) {
    fun toEffective(): EffectiveChatSettings {
        val temperatureValue = temperature.toFloatOrNull() ?: error("Temperature must be a number.")
        val topKValue = topK.toIntOrNull() ?: error("Top-K must be a whole number.")
        val topPValue = topP.toFloatOrNull() ?: error("Top-P must be a number.")
        val maxOutputValue = maxOutputTokens.toIntOrNull() ?: error("Maximum output tokens must be a whole number.")
        val seedValue = seed.trim().let { value ->
            if (value.isEmpty()) null else value.toIntOrNull() ?: error("Seed must be a whole number.")
        }
        val contextValue = contextSize.toIntOrNull() ?: error("Context size must be a whole number.")
        val threadsValue = threadCount.toIntOrNull() ?: error("Thread count must be a whole number.")
        require(temperatureValue in 0f..2f) { "Temperature must be between 0 and 2." }
        require(topKValue in 1..200) { "Top-K must be between 1 and 200." }
        require(topPValue in 0.05f..1f) { "Top-P must be between 0.05 and 1." }
        require(seedValue == null || seedValue >= 0) { "Seed cannot be negative; leave it blank for engine selection." }
        require(contextValue in 128..32_768) { "Context size must be between 128 and 32,768 tokens." }
        require(maxOutputValue in 1 until contextValue) {
            "Maximum output must be positive and smaller than the context size."
        }
        require(threadsValue in 0..64) { "Thread count must be between 0 and 64; 0 chooses a safe default." }
        return EffectiveChatSettings(
            computePreference = computePreference,
            systemPrompt = systemPrompt.trim(),
            temperature = temperatureValue,
            topK = topKValue,
            topP = topPValue,
            maxOutputTokens = maxOutputValue,
            seed = seedValue,
            contextSize = contextValue,
            threadCount = threadsValue,
        )
    }
}
