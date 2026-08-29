package com.dmitriim.localailab.ai.runtime.tts

import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRuntime
import com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutedTextToSpeechEngineTest {
    @Test
    fun switchingBackendsCancelsAndUnloadsThePreviousBackend() {
        val first = FakeSpeechRuntime("first")
        val second = FakeSpeechRuntime("second")
        val engine = RoutedTextToSpeechEngine(setOf(first, second))

        engine.load(requestFor("first"))
        engine.load(requestFor("second"))

        assertEquals(listOf("load", "cancel", "unload"), first.events)
        assertTrue(second.isLoaded)
        assertFalse(first.isLoaded)
    }

    @Test(expected = IllegalStateException::class)
    fun synthesisBeforeLoadFails() {
        RoutedTextToSpeechEngine(setOf(FakeSpeechRuntime("only"))).synthesize(
            TextToSpeechRequest(
                text = "hello",
                languageCode = "en",
                voice = TextToSpeechVoiceCondition.FixedSpeaker(0),
                speed = 1f,
                sentenceSilenceScale = 1f,
            ),
            onAudioChunk = { true },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateBackendIdsAreRejected() {
        RoutedTextToSpeechEngine(setOf(FakeSpeechRuntime("duplicate"), FakeSpeechRuntime("duplicate")))
    }

    private fun requestFor(id: String) = TextToSpeechLoadRequest(
        engineId = EngineId(id),
        profileType = ModelProfileId("PIPER_VITS_TTS"),
        modelDirectory = "model",
    )

    private class FakeSpeechRuntime(id: String) : TextToSpeechRuntime {
        override val engineId = EngineId(id)
        override var isLoaded = false
        val events = mutableListOf<String>()

        override fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult {
            events += "load"
            isLoaded = true
            return TextToSpeechLoadResult(1, 1, coldStart = true, sampleRateHz = 16_000, speakerCount = 1)
        }

        override fun synthesize(
            request: TextToSpeechRequest,
            onAudioChunk: (FloatArray) -> Boolean,
        ) = TextToSpeechResult(floatArrayOf(0f), 16_000)

        override fun cancel() {
            events += "cancel"
        }

        override fun unload() {
            events += "unload"
            isLoaded = false
        }
    }
}
