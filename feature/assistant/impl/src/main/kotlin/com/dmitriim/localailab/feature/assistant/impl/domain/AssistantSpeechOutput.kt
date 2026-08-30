package com.dmitriim.localailab.feature.assistant.impl.domain

import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localailab.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localailab.feature.assistant.impl.presentation.SpeechOutputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.TtsModelOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.TtsVoiceOption
import com.dmitriim.localailab.feature.tts.api.domain.PreviewSpeech
import com.dmitriim.localailab.feature.tts.api.domain.SpeechPreviewRequest
import com.dmitriim.localailab.feature.tts.api.domain.SpeechSynthesisEvent
import com.dmitriim.localailab.feature.tts.api.domain.SpeechSynthesisRequest
import com.dmitriim.localailab.feature.tts.api.domain.SpeechSynthesisSettings
import com.dmitriim.localailab.feature.tts.api.domain.SynthesizeSpeech
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
