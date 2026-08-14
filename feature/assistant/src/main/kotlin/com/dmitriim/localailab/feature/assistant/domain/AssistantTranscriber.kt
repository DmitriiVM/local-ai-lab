package com.dmitriim.localailab.feature.assistant.domain

import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.voice.stt.SpeechTranscriptionEvent
import com.dmitriim.localailab.core.voice.stt.SpeechTranscriptionRequest
import com.dmitriim.localailab.core.voice.stt.SttTranscriptionSettings
import com.dmitriim.localailab.core.voice.stt.TranscribeAudio
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
