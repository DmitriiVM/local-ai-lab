package com.dmitriim.localaiplayground.feature.assistant.presentation

import androidx.annotation.StringRes
import com.dmitriim.localaiplayground.ai.api.llm.LlmContextManagement
import com.dmitriim.localaiplayground.ai.api.llm.LlmEngineCapabilities
import com.dmitriim.localaiplayground.ai.api.llm.LlmFinishReason
import com.dmitriim.localaiplayground.core.audio.input.model.AudioLevel
import com.dmitriim.localaiplayground.core.audio.input.storage.ReferenceVoice
import com.dmitriim.localaiplayground.core.model.engine.ComputePreference
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId
import com.dmitriim.localaiplayground.core.model.manifest.TtsControl
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceMode
import com.dmitriim.localaiplayground.core.performance.InferenceTelemetry
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR
import java.util.UUID

data class AssistantUiState(
    val chatModels: List<ChatModelOption> = emptyList(),
    val speechModels: List<SpeechModelOption> = emptyList(),
    val voiceModels: List<TtsModelOption> = emptyList(),
    val selectedChatModelId: ModelId? = null,
    val selectedSpeechModelId: ModelId? = null,
    val selectedVoiceModelId: ModelId? = null,
    val selectedVoiceId: String? = null,
    val inputMode: AssistantInputMode = AssistantInputMode.DICTATE,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val chatSettings: ChatSettings = ChatSettings(),
    val speechInputSettings: SpeechInputSettings = SpeechInputSettings(),
    val speechOutputSettings: SpeechOutputSettings = SpeechOutputSettings(),
    val operation: AssistantOperation = AssistantOperation.Idle,
    val level: AudioLevel? = null,
    val speakingMessageId: String? = null,
    val contextUsage: ContextUsage? = null,
    val metrics: ChatMetrics? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    val selectedChatModel: ChatModelOption? get() = chatModels.firstOrNull { it.id == selectedChatModelId }
    val selectedSpeechModel: SpeechModelOption? get() = speechModels.firstOrNull { it.id == selectedSpeechModelId }
    val selectedVoiceModel: TtsModelOption? get() = voiceModels.firstOrNull { it.id == selectedVoiceModelId }
    val compatibleVoices: List<TtsVoiceOption>
        get() = selectedVoiceModel?.compatibleVoices(speechOutputSettings.languageCode).orEmpty()
    val selectedVoice: TtsVoiceOption? get() = compatibleVoices.firstOrNull { it.id == selectedVoiceId }
    val isIdle: Boolean get() = operation == AssistantOperation.Idle
    val canSend: Boolean
        get() = isIdle &&
            input.isNotBlank() &&
            selectedChatModel?.let { model ->
                model.installed && model.capabilities != null
            } == true
    val canDictate: Boolean get() = isIdle && selectedSpeechModel?.installed == true
    @get:StringRes
    val voiceConfigurationError: Int?
        get() = when {
            selectedChatModel?.installed != true -> CoreUiR.string.assistant_error_select_chat_model
            selectedSpeechModel?.installed != true -> CoreUiR.string.assistant_error_select_speech_model
            selectedVoiceModel?.installed != true -> CoreUiR.string.assistant_error_select_tts_model
            selectedVoice == null -> CoreUiR.string.assistant_error_select_compatible_voice
            else -> null
        }
}

enum class AssistantInputMode { DICTATE, VOICE }

sealed interface AssistantOperation {
    data object Idle : AssistantOperation
    data object Recording : AssistantOperation
    data object Transcribing : AssistantOperation
    data object Loading : AssistantOperation
    data object Generating : AssistantOperation
    data object Speaking : AssistantOperation
    data object Cancelling : AssistantOperation
}

data class ChatModelOption(
    val id: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val capabilities: LlmEngineCapabilities?,
    val defaultContextSize: Int,
    val installed: Boolean,
) {
    fun supportedComputePreference(preference: ComputePreference): ComputePreference {
        val supported = capabilities?.computePreferences.orEmpty()
        return preference.takeIf(supported::contains)
            ?: ComputePreference.AUTO.takeIf(supported::contains)
            ?: supported.firstOrNull()
            ?: preference
    }
}

data class SpeechModelOption(
    val id: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val languages: Set<String>,
    val sampleRateHz: Int,
    val installed: Boolean,
) {
    fun supports(languageCode: String): Boolean = languages.isEmpty() ||
        languages.any {
            normalizeLanguageCode(it) == languageCode
        }
}

data class TtsModelOption(
    val id: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val languages: Set<String>,
    val speakerCount: Int?,
    val voiceMode: TtsVoiceMode,
    val supportedControls: Set<TtsControl>,
    val voices: List<TtsVoiceOption>,
    val installed: Boolean,
) {
    fun compatibleVoices(languageCode: String): List<TtsVoiceOption> {
        if (languages.isNotEmpty() && languages.none { normalizeLanguageCode(it) == languageCode }) return emptyList()
        return voices.filter { voice ->
            voice.languages.isEmpty() || voice.languages.any { normalizeLanguageCode(it) == languageCode }
        }
    }
}

data class TtsVoiceOption(
    val id: String,
    val displayName: String,
    val speakerId: Int?,
    val languages: Set<String>,
    val description: String?,
    val reference: ReferenceVoice? = null,
    val platformVoiceId: String? = null,
)

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
        require(maxOutputValue in 1 until contextValue) { "Maximum output must be positive and smaller than the context size." }
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

data class SpeechInputSettings(
    val languageCode: String = "en",
    val threadCount: String = "0",
) {
    fun validate() {
        require(languageCode.isNotBlank()) { "Select a recognition language." }
        require(threadCount.toIntOrNull() in 0..64) { "Thread count must be between 0 and 64." }
    }
}

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

data class EffectiveChatSettings(
    val computePreference: ComputePreference,
    val systemPrompt: String,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
    val maxOutputTokens: Int,
    val seed: Int?,
    val contextSize: Int,
    val threadCount: Int,
)

data class ContextUsage(
    val promptTokens: Int?,
    val promptTokensEstimated: Boolean,
    val contextSize: Int?,
    val reservedOutputTokens: Int?,
    val omittedMessageCount: Int,
    val contextManagement: LlmContextManagement,
)

data class ChatMetrics(
    val modelName: String,
    val coldStart: Boolean,
    val loadDurationMs: Long,
    val promptTokens: Int?,
    val promptTokensPerSecond: Double?,
    val timeToFirstTokenMs: Long?,
    val generatedTokens: Int?,
    val generatedTokensPerSecond: Double?,
    val totalDurationMs: Long,
    val finishReason: LlmFinishReason,
    val effectiveSettings: EffectiveChatSettings,
    val effectiveThreadCount: Int?,
    val telemetry: InferenceTelemetry? = null,
)

internal fun AssistantUiState.replaceAssistantText(
    id: String,
    text: String,
    append: Boolean,
): AssistantUiState = copy(
    messages = messages.map { message ->
        if (message.id == id) {
            message.copy(content = if (append) message.content + text else text, streaming = append)
        } else {
            message
        }
    },
)

internal fun normalizeLanguageCode(value: String): String = when (val normalized = value.lowercase()) {
    "english" -> "en"
    "russian" -> "ru"
    "chinese" -> "zh"
    "japanese" -> "ja"
    "korean" -> "ko"
    "cantonese" -> "yue"
    else -> normalized.substringBefore('-').substringBefore('_')
}
