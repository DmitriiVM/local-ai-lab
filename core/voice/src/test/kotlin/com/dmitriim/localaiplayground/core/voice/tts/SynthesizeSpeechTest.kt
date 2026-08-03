package com.dmitriim.localaiplayground.core.voice.tts

import android.app.Application
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechEngine
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechLoadResult
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechResult
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localaiplayground.core.audio.output.api.SpeechPlaybackSession
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackMetrics
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackState
import com.dmitriim.localaiplayground.core.audio.output.storage.GeneratedAudioStore
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffectsProcessor
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceMode
import com.dmitriim.localaiplayground.core.model.runtime.ChatModelReference
import com.dmitriim.localaiplayground.core.model.runtime.SpeechToTextModelReference
import com.dmitriim.localaiplayground.core.model.runtime.TextToSpeechModelReference
import com.dmitriim.localaiplayground.core.model.service.LocalModelResolver
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SynthesizeSpeechTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `successful synthesis retains audio and releases completed playback`() = runBlocking {
        val player = FakePlayer()
        val store = generatedAudioStore()
        val synthesis = synthesizeSpeech(player, store)

        val events = synthesis.execute(request()).toList()

        assertEquals(3, events.size)
        assertTrue(events[1] is SpeechSynthesisEvent.Synthesized)
        assertTrue(events[2] is SpeechSynthesisEvent.Completed)
        assertEquals(listOf(true), player.releaseCalls)
        assertEquals(2, store.latest()?.sampleCount)
    }

    @Test
    fun `playback rejection cancels synthesis and discards its output`() = runBlocking {
        val player = FakePlayer(acceptWrites = false)
        val store = generatedAudioStore()
        val engine = FakeTextToSpeechEngine()
        val synthesis = synthesizeSpeech(player, store, engine)

        val outcome = runCatching { synthesis.execute(request()).toList() }

        assertTrue(outcome.isFailure)
        assertEquals(1, engine.cancelCalls)
        assertEquals(listOf(false), player.releaseCalls)
        assertEquals(null, store.latest())
    }

    @Test
    fun `replay streams retained PCM and releases completed playback`() = runBlocking {
        val player = FakePlayer()
        val store = generatedAudioStore()
        val retained = store.saveLatest(floatArrayOf(-1f, 1f), 16_000)
        val synthesis = synthesizeSpeech(player, store)

        synthesis.replay(retained, volume = 0.5f)

        assertEquals(listOf(true), player.releaseCalls)
        assertEquals(listOf(byteArrayOf(1, -128, -1, 127).asList()), player.sessions.single().pcmWrites)
    }

    private fun synthesizeSpeech(
        player: FakePlayer,
        store: GeneratedAudioStore,
        engine: FakeTextToSpeechEngine = FakeTextToSpeechEngine(),
    ) = SynthesizeSpeech(
        modelResolver = FakeModelResolver,
        textToSpeechEngine = engine,
        player = player,
        generatedAudioStore = store,
        audioEffectsProcessor = SpeechAudioEffectsProcessor(),
    )

    private fun generatedAudioStore(): GeneratedAudioStore {
        val directory = temporaryFolder.newFolder("generated-audio")
        return GeneratedAudioStore(TestApplication(directory))
    }

    private fun request() = SpeechSynthesisRequest(
        modelId = ModelId("model"),
        text = "Hello",
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

    private class TestApplication(private val storageDirectory: File) : Application() {
        override fun getFilesDir(): File = storageDirectory
    }

    private object FakeModelResolver : LocalModelResolver {
        override suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference> = error("Not used by TTS")

        override suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference> = error("Not used by TTS")

        override suspend fun resolveTextToSpeechModel(modelId: ModelId) = Result.success(
            TextToSpeechModelReference(
                modelId = modelId,
                displayName = "Test voice",
                engineId = EngineId("test"),
                profileType = ModelProfileId.PIPER_VITS_TTS,
                modelDirectory = "/models/test",
                sampleRateHz = 16_000,
                languages = setOf("en"),
                speakerCount = 1,
                voiceMode = TtsVoiceMode.SPEAKER_ID,
                supportedControls = emptySet(),
            ),
        )
    }

    private class FakeTextToSpeechEngine : TextToSpeechEngine {
        var cancelCalls = 0
        override val isLoaded = true

        override fun load(request: TextToSpeechLoadRequest) = TextToSpeechLoadResult(1, 0, false, 16_000, 1)

        override fun synthesize(
            request: TextToSpeechRequest,
            onAudioChunk: (FloatArray) -> Boolean,
        ): TextToSpeechResult {
            onAudioChunk(floatArrayOf(-1f, 1f))
            return TextToSpeechResult(floatArrayOf(-1f, 1f), 16_000)
        }

        override fun cancel() {
            cancelCalls += 1
        }

        override fun unload() = Unit
    }

    private class FakePlayer(
        private val acceptWrites: Boolean = true,
    ) : StreamingSpeechPlayer {
        override val state: StateFlow<SpeechPlaybackState> = MutableStateFlow(SpeechPlaybackState())
        val sessions = mutableListOf<FakeSession>()
        val releaseCalls = mutableListOf<Boolean>()

        override fun open(sampleRateHz: Int, volume: Float, runAnchorNanos: Long): SpeechPlaybackSession = FakeSession(acceptWrites).also(sessions::add)

        override fun pause() = Unit

        override fun resume() = Unit

        override fun stop() = Unit

        override fun release(completed: Boolean) {
            releaseCalls += completed
        }
    }

    private class FakeSession(
        private val acceptWrites: Boolean,
    ) : SpeechPlaybackSession {
        override val sampleRateHz = 16_000
        val pcmWrites = mutableListOf<List<Byte>>()

        override fun write(samples: FloatArray) = acceptWrites

        override fun writePcm16(pcm16: ByteArray): Boolean {
            pcmWrites += pcm16.asList()
            return acceptWrites
        }

        override suspend fun awaitDrained() = Unit

        override fun metrics() = SpeechPlaybackMetrics(null, null, 2, 2, 0)
    }
}
