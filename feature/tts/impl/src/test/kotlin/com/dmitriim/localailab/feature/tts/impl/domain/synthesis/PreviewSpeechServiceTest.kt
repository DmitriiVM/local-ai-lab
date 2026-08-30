package com.dmitriim.localailab.feature.tts.impl.domain.synthesis

import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceMode
import com.dmitriim.localailab.ai.api.model.runtime.ChatModelReference
import com.dmitriim.localailab.ai.api.tts.TextToSpeechEngine
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localailab.core.audio.output.api.SpeechPlaybackSession
import com.dmitriim.localailab.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localailab.core.audio.output.model.SpeechPlaybackMetrics
import com.dmitriim.localailab.core.audio.output.model.SpeechPlaybackState
import com.dmitriim.localailab.core.audio.processing.SpeechAudioEffectsProcessor
import com.dmitriim.localailab.feature.models.api.domain.runtime.LocalModelResolver
import com.dmitriim.localailab.feature.models.api.domain.runtime.SpeechToTextModelReference
import com.dmitriim.localailab.feature.models.api.domain.runtime.TextToSpeechModelReference
import com.dmitriim.localailab.feature.tts.api.domain.SpeechPreviewRequest
import com.dmitriim.localailab.feature.tts.api.domain.SpeechSynthesisSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSpeechServiceTest {
    @Test
    fun `successful preview releases completed playback`() = runBlocking {
        val player = FakePlayer(FakeSession())
        val preview = previewSpeech(player)

        preview.execute(previewRequest())

        assertEquals(listOf(true), player.releaseCalls)
    }

    @Test
    fun `preview failure releases incomplete playback`() = runBlocking {
        val player = FakePlayer(FakeSession(drainFailure = IllegalStateException("playback failed")))
        val preview = previewSpeech(player)

        runCatching { preview.execute(previewRequest()) }

        assertEquals(listOf(false), player.releaseCalls)
    }

    private fun previewSpeech(player: FakePlayer) = PreviewSpeechService(
        modelResolver = FakeModelResolver,
        textToSpeechEngine = FakeTextToSpeechEngine,
        player = player,
        audioEffectsProcessor = SpeechAudioEffectsProcessor(),
    )

    private fun previewRequest() = SpeechPreviewRequest(
        modelId = ModelId("model"),
        text = "Hello",
        voiceName = "Speaker 0",
        settings = SpeechSynthesisSettings(
            languageCode = "en",
            voiceCondition = TextToSpeechVoiceCondition.FixedSpeaker(0),
            expectedSpeakerCount = 1,
            speed = 1f,
            sentenceSilenceScale = 1f,
            volume = 1f,
            threadCount = 1,
        ),
    )

    private object FakeModelResolver : LocalModelResolver {
        override suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference> = error("Not used by a speech preview")

        override suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference> = error("Not used by a speech preview")

        override suspend fun resolveTextToSpeechModel(modelId: ModelId) = Result.success(
            TextToSpeechModelReference(
                modelId = modelId,
                displayName = "Test voice",
                engineId = EngineId("test"),
                profileType = ModelProfileId("PIPER_VITS_TTS"),
                modelDirectory = "/models/test",
                sampleRateHz = 16_000,
                languages = setOf("en"),
                speakerCount = 1,
                voiceMode = TtsVoiceMode.SPEAKER_ID,
                supportedControls = emptySet(),
            ),
        )
    }

    private object FakeTextToSpeechEngine : TextToSpeechEngine {
        override val isLoaded = true

        override fun load(request: TextToSpeechLoadRequest) = TextToSpeechLoadResult(
            effectiveThreadCount = 1,
            loadDurationMs = 0,
            coldStart = false,
            sampleRateHz = 16_000,
            speakerCount = 1,
        )

        override fun synthesize(
            request: TextToSpeechRequest,
            onAudioChunk: (FloatArray) -> Boolean,
        ): TextToSpeechResult {
            check(onAudioChunk(floatArrayOf(0.25f, -0.25f)))
            return TextToSpeechResult(floatArrayOf(0.25f, -0.25f), 16_000)
        }

        override fun cancel() = Unit

        override fun unload() = Unit
    }

    private class FakePlayer(
        private val session: FakeSession,
    ) : StreamingSpeechPlayer {
        override val state: StateFlow<SpeechPlaybackState> = MutableStateFlow(SpeechPlaybackState())
        val releaseCalls = mutableListOf<Boolean>()

        override fun open(sampleRateHz: Int, volume: Float, runAnchorNanos: Long) = session

        override fun pause() = Unit

        override fun resume() = Unit

        override fun stop() = Unit

        override fun release(completed: Boolean) {
            releaseCalls += completed
        }
    }

    private class FakeSession(
        private val drainFailure: Throwable? = null,
    ) : SpeechPlaybackSession {
        override val sampleRateHz = 16_000

        override fun write(samples: FloatArray) = true

        override fun writePcm16(pcm16: ByteArray) = true

        override suspend fun awaitDrained() {
            drainFailure?.let { throw it }
        }

        override fun metrics() = SpeechPlaybackMetrics(null, null, 0, 0, 0)
    }
}
