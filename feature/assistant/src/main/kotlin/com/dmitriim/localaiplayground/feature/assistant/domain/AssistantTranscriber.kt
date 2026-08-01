package com.dmitriim.localaiplayground.feature.assistant.domain

import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.feature.stt.domain.SpeechTranscriptionEvent
import com.dmitriim.localaiplayground.feature.stt.domain.SpeechTranscriptionRequest
import com.dmitriim.localaiplayground.feature.stt.domain.SttTranscriptionSettings
import com.dmitriim.localaiplayground.feature.stt.domain.TranscribeAudio
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class AssistantTranscriber(
    private val transcribeAudio: TranscribeAudio,
) {
    fun transcribe(
        modelId: ModelId,
        input: PcmAudioInput,
        languageCode: String,
        threadCount: String,
    ): Flow<SpeechTranscriptionEvent> = transcribeAudio.execute(
        SpeechTranscriptionRequest(
            modelId = modelId,
            input = input,
            settings = SttTranscriptionSettings(languageCode, threadCount),
        ),
    )

    fun cancel() = transcribeAudio.cancel()
}
