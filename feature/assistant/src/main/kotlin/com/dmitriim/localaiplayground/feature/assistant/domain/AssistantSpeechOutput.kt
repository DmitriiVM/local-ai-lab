package com.dmitriim.localaiplayground.feature.assistant.domain

import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.feature.assistant.presentation.SpeechOutputSettings
import com.dmitriim.localaiplayground.feature.assistant.presentation.TtsModelOption
import com.dmitriim.localaiplayground.feature.assistant.presentation.TtsVoiceOption
import com.dmitriim.localaiplayground.feature.tts.domain.PreviewSpeech
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechPreviewRequest
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisEvent
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisRequest
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisSettings
import com.dmitriim.localaiplayground.feature.tts.domain.SynthesizeSpeech
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class AssistantSpeechOutput(
    private val synthesizeSpeech: SynthesizeSpeech,
    private val previewSpeech: PreviewSpeech,
) {
    fun speak(
        modelId: ModelId,
        model: TtsModelOption,
        voice: TtsVoiceOption,
        text: String,
        settings: SpeechOutputSettings,
    ): Flow<SpeechSynthesisEvent> = synthesizeSpeech.execute(
        SpeechSynthesisRequest(
            modelId = modelId,
            text = text,
            settings = settings.toDomain(model, voice),
        ),
    )

    suspend fun preview(
        modelId: ModelId,
        model: TtsModelOption,
        voice: TtsVoiceOption,
        settings: SpeechOutputSettings,
    ) {
        previewSpeech.execute(
            SpeechPreviewRequest(
                modelId = modelId,
                text = "This is how assistant responses will sound.",
                voiceName = voice.displayName,
                settings = settings.toDomain(model, voice),
            ),
        )
    }

    fun cancel() {
        previewSpeech.cancel()
        synthesizeSpeech.cancel()
    }

    fun unload() = synthesizeSpeech.unloadRuntime()
}

private fun SpeechOutputSettings.toDomain(
    model: TtsModelOption,
    voice: TtsVoiceOption,
): SpeechSynthesisSettings {
    validate()
    return SpeechSynthesisSettings(
        languageCode = languageCode,
        voiceCondition = voice.toCondition(),
        voiceName = voice.displayName,
        expectedSpeakerCount = model.speakerCount,
        speed = requireNotNull(speed.toFloatOrNull()),
        sentenceSilenceScale = requireNotNull(sentenceSilenceScale.toFloatOrNull()),
        volume = requireNotNull(volume.toFloatOrNull()),
        threadCount = requireNotNull(threadCount.toIntOrNull()),
        audioEffects = SpeechAudioEffects(),
    )
}

private fun TtsVoiceOption.toCondition(): TextToSpeechVoiceCondition = platformVoiceId?.let(TextToSpeechVoiceCondition::PlatformVoice)
    ?: reference?.let { reference ->
        TextToSpeechVoiceCondition.ReferenceAudio(
            referenceId = reference.id,
            displayName = reference.displayName,
            pcmFilePath = reference.pcmFilePath,
            sampleRateHz = reference.sampleRateHz,
        )
    }
    ?: TextToSpeechVoiceCondition.FixedSpeaker(
        requireNotNull(speakerId) { "The selected fixed voice has no speaker ID." },
    )
