package com.dmitriim.localaiplayground.feature.chat.presentation

import com.dmitriim.localaiplayground.ai.api.LlmFinishReason
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import java.util.UUID

data class ChatUiState(
    val availableModels: List<ChatModelOption> = emptyList(),
    val selectedModelId: ModelId? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val settings: ChatSettings = ChatSettings(),
    val operation: ChatOperation = ChatOperation.IDLE,
    val contextUsage: ContextUsage? = null,
    val metrics: ChatMetrics? = null,
    val errorMessage: String? = null,
)

data class ChatModelOption(val id: ModelId, val displayName: String, val defaultContextSize: Int)

enum class ChatMessageRole { USER, ASSISTANT }

data class ChatMessage(
    val id: String,
    val role: ChatMessageRole,
    val content: String,
    val streaming: Boolean = false,
    val failed: Boolean = false,
) {
    companion object {
        fun user(content: String) = ChatMessage(UUID.randomUUID().toString(), ChatMessageRole.USER, content)
        fun assistant(id: String, content: String, streaming: Boolean) = ChatMessage(id, ChatMessageRole.ASSISTANT, content, streaming)
    }
}

enum class ChatOperation { IDLE, LOADING, GENERATING, CANCELLING }

data class ChatSettings(
    val systemPrompt: String = "You are a helpful, concise assistant.",
    val temperature: String = "0.7",
    val topK: String = "40",
    val topP: String = "0.9",
    val maxOutputTokens: String = "128",
    val seed: String = "-1",
    val contextSize: String = "512",
    val threadCount: String = "0",
) {
    fun toEffective(): EffectiveChatSettings {
        val temperatureValue = temperature.toFloatOrNull() ?: error("Temperature must be a number.")
        val topKValue = topK.toIntOrNull() ?: error("Top-K must be a whole number.")
        val topPValue = topP.toFloatOrNull() ?: error("Top-P must be a number.")
        val maxOutputValue = maxOutputTokens.toIntOrNull() ?: error("Maximum output tokens must be a whole number.")
        val seedValue = seed.toIntOrNull() ?: error("Seed must be a whole number.")
        val contextValue = contextSize.toIntOrNull() ?: error("Context size must be a whole number.")
        val threadsValue = threadCount.toIntOrNull() ?: error("Thread count must be a whole number.")
        require(temperatureValue in 0f..2f) { "Temperature must be between 0 and 2." }
        require(topKValue in 1..200) { "Top-K must be between 1 and 200." }
        require(topPValue in 0.05f..1f) { "Top-P must be between 0.05 and 1." }
        require(contextValue in 128..32_768) { "Context size must be between 128 and 32,768 tokens." }
        require(maxOutputValue in 1 until contextValue) { "Maximum output must be positive and smaller than the context size." }
        require(threadsValue in 0..64) { "Thread count must be between 0 and 64; 0 chooses a safe default." }
        return EffectiveChatSettings(systemPrompt.trim(), temperatureValue, topKValue, topPValue, maxOutputValue, seedValue, contextValue, threadsValue)
    }
}

data class EffectiveChatSettings(
    val systemPrompt: String,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
    val maxOutputTokens: Int,
    val seed: Int,
    val contextSize: Int,
    val threadCount: Int,
)

data class ContextUsage(val promptTokens: Int, val contextSize: Int, val reservedOutputTokens: Int, val omittedMessageCount: Int)

data class ChatMetrics(
    val modelName: String,
    val coldStart: Boolean,
    val loadDurationMs: Long,
    val promptTokens: Int,
    val promptTokensPerSecond: Double?,
    val timeToFirstTokenMs: Long?,
    val generatedTokens: Int,
    val generatedTokensPerSecond: Double?,
    val totalDurationMs: Long,
    val finishReason: LlmFinishReason,
    val effectiveSettings: EffectiveChatSettings,
    val effectiveThreadCount: Int,
)

internal fun isReadyChatModel(model: InstalledModel): Boolean =
    AiCapability.CHAT in model.manifest.capabilities && model.validationState == ModelValidationState.READY

internal fun InstalledModel.toChatModelOption() =
    ChatModelOption(manifest.modelId, manifest.displayName, manifest.contextSize ?: 512)

internal fun ChatUiState.replaceAssistantText(id: String, text: String, append: Boolean): ChatUiState = copy(
    messages = messages.map { message ->
        if (message.id == id) message.copy(content = if (append) message.content + text else text, streaming = append) else message
    },
)
