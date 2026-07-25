package com.dmitriim.localaiplayground.feature.voice.domain

import com.dmitriim.localaiplayground.core.model.ModelRepository

/** Revalidates the selected profiles before the microphone is opened. */
internal suspend fun ModelRepository.preflightVoicePipeline(request: VoiceTurnRequest): PreparedVoicePipeline {
    request.validate()
    val speech = resolveSpeechToTextModel(request.speechModelId).getOrThrow()
    val chat = resolveChatModel(request.chatModelId).getOrThrow()
    val voice = resolveTextToSpeechModel(request.voiceModelId).getOrThrow()
    require(request.languageCode in speech.languages) {
        "${speech.displayName} is not configured for ${request.languageCode}."
    }
    require(request.languageCode in voice.languages) {
        "${voice.displayName} is not configured for ${request.languageCode}."
    }
    return PreparedVoicePipeline(speech, chat, voice)
}
