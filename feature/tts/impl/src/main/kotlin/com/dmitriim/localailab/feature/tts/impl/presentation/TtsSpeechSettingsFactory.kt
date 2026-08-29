package com.dmitriim.localailab.feature.tts.impl.presentation

import com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localailab.feature.tts.api.domain.SpeechSynthesisSettings

internal object TtsSpeechSettingsFactory {
    fun create(
        state: TextToSpeechUiState,
        voice: TtsVoiceOption,
        threadCount: Int,
    ) = SpeechSynthesisSettings(
        languageCode = state.language.code,
        voiceCondition = voiceCondition(voice),
        voiceName = voice.displayName,
        expectedSpeakerCount = state.selectedModel?.speakerCount,
        speed = state.speed,
        sentenceSilenceScale = state.sentenceSilenceScale,
        volume = state.volume,
        threadCount = threadCount,
        audioEffects = state.audioEffects,
    )

    private fun voiceCondition(voice: TtsVoiceOption): TextToSpeechVoiceCondition = voice.platformVoiceId
        ?.let(TextToSpeechVoiceCondition::PlatformVoice)
        ?: voice.reference?.let { reference ->
            TextToSpeechVoiceCondition.ReferenceAudio(
                referenceId = reference.id,
                displayName = reference.displayName,
                pcmFilePath = reference.pcmFilePath,
                sampleRateHz = reference.sampleRateHz,
            )
        } ?: TextToSpeechVoiceCondition.FixedSpeaker(
        requireNotNull(voice.speakerId) { "The selected fixed voice has no speaker ID." },
    )
}
