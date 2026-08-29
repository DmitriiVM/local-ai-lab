package com.dmitriim.localailab.feature.stt.impl.domain.transcription

import android.app.Application
import com.dmitriim.localailab.ai.api.stt.SpeechToTextEngine
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadResult
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextResult
import com.dmitriim.localailab.core.audio.input.android.MicrophoneCapture
import com.dmitriim.localailab.core.audio.input.android.PlatformAudioDecoder
import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput
import com.dmitriim.localailab.core.audio.input.storage.AudioInputStore
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.SttRecognitionMode
import com.dmitriim.localailab.core.model.runtime.ChatModelReference
import com.dmitriim.localailab.feature.models.api.domain.runtime.SpeechToTextModelReference
import com.dmitriim.localailab.feature.models.api.domain.runtime.TextToSpeechModelReference
import com.dmitriim.localailab.feature.models.api.domain.runtime.LocalModelResolver
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionEvent
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionRequest
import com.dmitriim.localailab.feature.stt.api.domain.SttTranscriptionSettings
import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TranscribeAudioServiceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `transcription joins nonblank segments and aggregates their metrics`() = runBlocking {
        val engine = FakeSpeechToTextEngine(listOf("First" to 10L, "" to 20L, "second" to 30L))
        val transcription = TranscribeAudioService(FakeModelResolver, engine, audioInputStore())

        val events = transcription.execute(request()).toList()

        val completed = events.last() as SpeechTranscriptionEvent.Completed
        assertEquals("First second", completed.transcript)
        assertEquals(3, completed.metrics.segmentCount)
        assertEquals(60L, completed.metrics.processingDurationMs)
        assertEquals(1, engine.unloadCalls)
    }

    @Test
    fun `transcription unloads the engine after a segment failure`() = runBlocking {
        val engine = FakeSpeechToTextEngine(listOf("First" to 10L), failure = IllegalStateException("decoder failed"))
        val transcription = TranscribeAudioService(FakeModelResolver, engine, audioInputStore())

        val outcome = runCatching { transcription.execute(request()).toList() }

        assertTrue(outcome.isFailure)
        assertEquals(1, engine.unloadCalls)
    }

    private fun request() = SpeechTranscriptionRequest(
        modelId = ModelId("model"),
        input = pcmInput(),
        settings = SttTranscriptionSettings(languageCode = "en", threadCount = "1"),
    )

    private fun pcmInput(): PcmAudioInput {
        val file = temporaryFolder.newFile("transcription.pcm")
        file.writeBytes(ByteArray(16_000 * 60 * 2 + 4))
        return PcmAudioInput(
            file = file,
            displayName = "input",
            durationMs = 60_000L,
            sampleRateHz = 16_000,
            sourceDescription = "test",
        )
    }

    private fun audioInputStore(): AudioInputStore {
        val application = TestApplication(temporaryFolder.newFolder("stt-cache"))
        return AudioInputStore(
            application = application,
            microphoneCapture = MicrophoneCapture(application),
            decoder = PlatformAudioDecoder(application),
        )
    }

    private class TestApplication(private val cacheDirectory: File) : Application() {
        override fun getCacheDir(): File = cacheDirectory
    }

    private object FakeModelResolver : LocalModelResolver {
        override suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference> = error("Not used by STT")

        override suspend fun resolveSpeechToTextModel(modelId: ModelId) = Result.success(
            SpeechToTextModelReference(
                modelId = modelId,
                displayName = "Test transcription",
                engineId = EngineId("test"),
                profileType = ModelProfileId("WHISPER_STT"),
                modelDirectory = "/models/test",
                files = emptyMap(),
                sampleRateHz = 16_000,
                languages = setOf("en"),
                recognitionMode = SttRecognitionMode.OFFLINE,
            ),
        )

        override suspend fun resolveTextToSpeechModel(modelId: ModelId): Result<TextToSpeechModelReference> = error("Not used by STT")
    }

    private class FakeSpeechToTextEngine(
        private val results: List<Pair<String, Long>>,
        private val failure: Throwable? = null,
    ) : SpeechToTextEngine {
        private var nextResult = 0
        var unloadCalls = 0
        override val isLoaded = true

        override fun load(request: SpeechToTextLoadRequest) = SpeechToTextLoadResult(1, 0, false)

        override fun transcribe(request: SpeechToTextRequest): SpeechToTextResult {
            failure?.let { throw it }
            val (text, durationMs) = results[nextResult++]
            return SpeechToTextResult(text, durationMs)
        }

        override fun cancel() = Unit

        override fun unload() {
            unloadCalls += 1
        }
    }
}
