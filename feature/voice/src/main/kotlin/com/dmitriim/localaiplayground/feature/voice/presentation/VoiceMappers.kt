package com.dmitriim.localaiplayground.feature.voice.presentation

import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.CatalogModel
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import com.dmitriim.localaiplayground.core.model.TtsVoiceMode
import com.dmitriim.localaiplayground.core.model.BuiltInSpeechToTextModels
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.feature.voice.domain.VoiceContextUsage as DomainVoiceContextUsage
import com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineEvent
import com.dmitriim.localaiplayground.feature.voice.domain.VoiceTurnPhase
import com.dmitriim.localaiplayground.feature.voice.domain.VoiceTurnRequest

internal fun VoiceUiState.toVoiceTurnRequest(): Result<VoiceTurnRequest> = runCatching {
    val effectiveSettings = settings
    fun integer(value: String, label: String): Int = value.toIntOrNull()
        ?: error("$label must be a whole number.")
    val request = VoiceTurnRequest(
        speechModelId = requireNotNull(selectedSpeechModelId),
        chatModelId = requireNotNull(selectedChatModelId),
        voiceModelId = requireNotNull(selectedVoiceModelId),
        languageCode = language.code,
        systemPrompt = effectiveSettings.systemPrompt.trim(),
        temperature = effectiveSettings.temperature.toFloatOrNull() ?: error("Temperature must be a number."),
        maxOutputTokens = integer(effectiveSettings.maxOutputTokens, "Maximum output"),
        contextSize = integer(effectiveSettings.contextSize, "Context size"),
        sttThreadCount = integer(effectiveSettings.sttThreadCount, "STT thread count"),
        llmThreadCount = integer(effectiveSettings.llmThreadCount, "LLM thread count"),
        ttsThreadCount = integer(effectiveSettings.ttsThreadCount, "TTS thread count"),
        speakerId = integer(effectiveSettings.speakerId, "Speaker ID"),
        speechRate = effectiveSettings.speechRate.toFloatOrNull() ?: error("Speech rate must be a number."),
        volume = effectiveSettings.volume.toFloatOrNull() ?: error("Playback volume must be a number."),
        history = conversation,
    )
    request.validate()
    request
}

internal fun VoiceUiState.withAvailableModels(
    installed: List<InstalledModel>,
    catalog: List<CatalogModel>,
    includeAndroidRecognizer: Boolean,
): VoiceUiState {
    val speech = buildList {
        if (includeAndroidRecognizer) {
            add(
                VoiceModelOption(
                    id = BuiltInSpeechToTextModels.ANDROID_SPEECH_RECOGNIZER,
                    displayName = "Android On-device SpeechRecognizer",
                    engineId = EngineId("android-speech-recognizer"),
                    languages = linkedSetOf("English", "Russian"),
                    approximateRamBytes = null,
                    installed = true,
                ),
            )
        }
        addAll(voiceModelOptions(installed, catalog, AiCapability.SPEECH_TO_TEXT))
    }
    val chat = voiceModelOptions(installed, catalog, AiCapability.CHAT)
    val voice = voiceModelOptions(installed, catalog, AiCapability.TEXT_TO_SPEECH) {
        it.ttsVoiceMode == TtsVoiceMode.SPEAKER_ID
    }
    return copy(
        speechModels = speech,
        chatModels = chat,
        voiceModels = voice,
        selectedSpeechModelId = selectedSpeechModelId.takeIf { id -> speech.any { it.id == id && it.installed } }
            ?: speech.firstOrNull { it.installed }?.id,
        selectedChatModelId = selectedChatModelId.takeIf { id -> chat.any { it.id == id && it.installed } }
            ?: chat.firstOrNull { it.installed }?.id,
        selectedVoiceModelId = selectedVoiceModelId.takeIf { id -> voice.any { it.id == id && it.installed } }
            ?: voice.firstOrNull { it.installed }?.id,
    )
}

internal fun VoiceUiState.reduce(event: VoicePipelineEvent): VoiceUiState = when (event) {
    is VoicePipelineEvent.Prepared -> copy(
        statusMessage = "Pipeline ready: ${event.pipeline.speechModel} → ${event.pipeline.chatModel} → ${event.pipeline.voiceModel}",
    )
    is VoicePipelineEvent.Phase -> copy(
        phase = event.value.toUiPhase(),
        level = if (event.value == VoiceTurnPhase.LISTENING) level else null,
        statusMessage = event.value.toStatusMessage(),
    )
    is VoicePipelineEvent.Level -> copy(level = event.value)
    is VoicePipelineEvent.FinalTranscript -> copy(finalTranscript = event.value)
    is VoicePipelineEvent.ContextPrepared -> copy(contextUsage = event.value.toUi())
    is VoicePipelineEvent.AssistantToken -> copy(streamingResponse = streamingResponse + event.value)
    is VoicePipelineEvent.AssistantCompleted -> copy(streamingResponse = event.value)
    is VoicePipelineEvent.Completed -> copy(metrics = event.metrics)
}

private fun DomainVoiceContextUsage.toUi() = VoiceContextUsage(
    promptTokens = promptTokens,
    contextSize = contextSize,
    reservedOutputTokens = reservedOutputTokens,
    omittedTurnCount = omittedTurnCount,
)

private fun VoiceTurnPhase.toUiPhase() = when (this) {
    VoiceTurnPhase.LISTENING -> VoicePhase.LISTENING
    VoiceTurnPhase.FINALIZING -> VoicePhase.FINALIZING
    VoiceTurnPhase.THINKING -> VoicePhase.THINKING
    VoiceTurnPhase.SPEAKING -> VoicePhase.SPEAKING
}

private fun VoiceTurnPhase.toStatusMessage() = when (this) {
    VoiceTurnPhase.LISTENING -> "Listening locally. Tap stop when you finish speaking."
    VoiceTurnPhase.FINALIZING -> "Transcribing the completed recording locally…"
    VoiceTurnPhase.THINKING -> "Generating a local response…"
    VoiceTurnPhase.SPEAKING -> "Synthesizing and playing the local response…"
}

private fun InstalledModel.toVoiceOption() = VoiceModelOption(
    id = manifest.modelId,
    displayName = manifest.displayName,
    engineId = manifest.engineId,
    languages = manifest.languages,
    approximateRamBytes = manifest.approximateRamBytes,
    installed = true,
)

private fun ModelManifest.toVoiceOption(installed: Boolean) = VoiceModelOption(
    id = modelId,
    displayName = displayName,
    engineId = engineId,
    languages = languages,
    approximateRamBytes = approximateRamBytes,
    installed = installed,
)

private fun voiceModelOptions(
    installedModels: List<InstalledModel>,
    catalogModels: List<CatalogModel>,
    capability: AiCapability,
    include: (ModelManifest) -> Boolean = { true },
): List<VoiceModelOption> {
    val installedById = installedModels
        .filter { it.validationState == ModelValidationState.READY }
        .filter { capability in it.manifest.capabilities && include(it.manifest) }
        .associateBy { it.manifest.modelId }
    val catalogEntries = catalogModels.filter {
        capability in it.manifest.capabilities && include(it.manifest)
    }
    return buildList {
        catalogEntries.forEach { entry ->
            add(installedById[entry.manifest.modelId]?.toVoiceOption() ?: entry.manifest.toVoiceOption(installed = false))
        }
        installedById
            .filterKeys { id -> catalogEntries.none { it.manifest.modelId == id } }
            .values
            .mapTo(this, InstalledModel::toVoiceOption)
    }
}

private fun isReadySpeech(model: InstalledModel) =
        model.validationState == ModelValidationState.READY &&
        AiCapability.SPEECH_TO_TEXT in model.manifest.capabilities

private fun isReadyChat(model: InstalledModel) =
        model.validationState == ModelValidationState.READY &&
        AiCapability.CHAT in model.manifest.capabilities

private fun isReadyVoice(model: InstalledModel) =
        model.validationState == ModelValidationState.READY &&
        AiCapability.TEXT_TO_SPEECH in model.manifest.capabilities &&
        model.manifest.ttsVoiceMode == TtsVoiceMode.SPEAKER_ID
