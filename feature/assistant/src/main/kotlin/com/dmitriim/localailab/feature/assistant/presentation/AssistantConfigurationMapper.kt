package com.dmitriim.localailab.feature.assistant.presentation

import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.feature.settings.api.domain.AssistantChatPreferences
import com.dmitriim.localailab.feature.settings.api.domain.AssistantPreferences
import com.dmitriim.localailab.feature.settings.api.domain.AssistantSpeechInputPreferences
import com.dmitriim.localailab.feature.settings.api.domain.AssistantSpeechOutputPreferences

internal data class AssistantModelOptions(
    val chat: List<ChatModelOption>,
    val speech: List<SpeechModelOption>,
    val voice: List<TtsModelOption>,
)

internal fun AssistantUiState.withConfiguration(
    options: AssistantModelOptions,
    preferences: AssistantPreferences,
): AssistantUiState {
    val chatId = preferences.chat.modelId?.let(::ModelId)
        ?.takeIf { id -> options.chat.any { it.id == id && it.installed } }
        ?: selectedChatModelId?.takeIf { id -> options.chat.any { it.id == id && it.installed } }
        ?: options.chat.firstOrNull { it.installed }?.id
    val speechId = preferences.speechInput.modelId?.let(::ModelId)
        ?.takeIf { id -> options.speech.any { it.id == id && it.installed } }
        ?: selectedSpeechModelId?.takeIf { id -> options.speech.any { it.id == id && it.installed } }
        ?: options.speech.firstOrNull { it.installed }?.id
    val speechModel = options.speech.firstOrNull { it.id == speechId }
    val speechLanguage = preferences.speechInput.languageCode.takeIf { speechModel?.supports(it) == true }
        ?: speechModel?.languages?.firstOrNull()?.let(::normalizeLanguageCode)
        ?: "en"
    val voiceId = preferences.speechOutput.modelId?.let(::ModelId)
        ?.takeIf { id -> options.voice.any { it.id == id && it.installed } }
        ?: selectedVoiceModelId?.takeIf { id -> options.voice.any { it.id == id && it.installed } }
        ?: options.voice.firstOrNull { it.installed }?.id
    val voiceModel = options.voice.firstOrNull { it.id == voiceId }
    val outputLanguage = preferences.speechOutput.languageCode.takeIf { language ->
        voiceModel?.languages?.isEmpty() == true || voiceModel?.languages?.any { normalizeLanguageCode(it) == language } == true
    } ?: voiceModel?.languages?.firstOrNull()?.let(::normalizeLanguageCode) ?: "en"
    val compatibleVoices = voiceModel?.compatibleVoices(outputLanguage).orEmpty()
    val selectedOutputVoice = preferences.speechOutput.voiceId?.takeIf { id -> compatibleVoices.any { it.id == id } }
        ?: selectedVoiceId?.takeIf { id -> compatibleVoices.any { it.id == id } }
        ?: compatibleVoices.firstOrNull()?.id
    val chatModel = options.chat.firstOrNull { it.id == chatId }
    val restoredChatSettings = preferences.chat.toUi()
    return copy(
        chatModels = options.chat,
        speechModels = options.speech,
        voiceModels = options.voice,
        selectedChatModelId = chatId,
        selectedSpeechModelId = speechId,
        selectedVoiceModelId = voiceId,
        selectedVoiceId = selectedOutputVoice,
        chatSettings = restoredChatSettings.copy(
            computePreference = chatModel?.supportedComputePreference(restoredChatSettings.computePreference)
                ?: restoredChatSettings.computePreference,
        ),
        speechInputSettings = SpeechInputSettings(speechLanguage, preferences.speechInput.threadCount.toString()),
        speechOutputSettings = SpeechOutputSettings(
            languageCode = outputLanguage,
            speed = preferences.speechOutput.speed.toString(),
            volume = preferences.speechOutput.volume.toString(),
            sentenceSilenceScale = preferences.speechOutput.sentenceSilenceScale.toString(),
            threadCount = preferences.speechOutput.threadCount.toString(),
        ),
    )
}

internal fun AssistantUiState.toPreferences() = AssistantPreferences(
    chat = chatSettings.toEffectiveOrDefault(selectedChatModel?.defaultContextSize ?: 512).let { settings ->
        AssistantChatPreferences(
            modelId = selectedChatModelId?.value,
            computePreference = settings.computePreference,
            systemPrompt = settings.systemPrompt,
            temperature = settings.temperature,
            topK = settings.topK,
            topP = settings.topP,
            maxOutputTokens = settings.maxOutputTokens,
            seed = settings.seed,
            contextSize = settings.contextSize,
            threadCount = settings.threadCount,
        )
    },
    speechInput = AssistantSpeechInputPreferences(
        modelId = selectedSpeechModelId?.value,
        languageCode = speechInputSettings.languageCode,
        threadCount = speechInputSettings.threadCount.toIntOrNull()?.coerceIn(0, 64) ?: 0,
    ),
    speechOutput = AssistantSpeechOutputPreferences(
        modelId = selectedVoiceModelId?.value,
        voiceId = selectedVoiceId,
        languageCode = speechOutputSettings.languageCode,
        speed = speechOutputSettings.speed.toFloatOrNull()?.coerceIn(0.5f, 2f) ?: 1f,
        volume = speechOutputSettings.volume.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f,
        sentenceSilenceScale = speechOutputSettings.sentenceSilenceScale.toFloatOrNull()?.coerceIn(0f, 2f) ?: 1f,
        threadCount = speechOutputSettings.threadCount.toIntOrNull()?.coerceIn(0, 64) ?: 0,
    ),
)

private fun AssistantChatPreferences.toUi() = ChatSettings(
    computePreference = computePreference,
    systemPrompt = systemPrompt,
    temperature = temperature.toString(),
    topK = topK.toString(),
    topP = topP.toString(),
    maxOutputTokens = maxOutputTokens.toString(),
    seed = seed?.toString().orEmpty(),
    contextSize = contextSize.toString(),
    threadCount = threadCount.toString(),
)

private fun ChatSettings.toEffectiveOrDefault(contextSize: Int): EffectiveChatSettings = runCatching(::toEffective).getOrElse {
    ChatSettings(contextSize = contextSize.toString()).toEffective()
}
