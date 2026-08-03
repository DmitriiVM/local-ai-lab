package com.dmitriim.localaiplayground.ai.api.stt

import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutedSpeechToTextEngineTest {
    @Test
    fun switchingBackendsCancelsAndUnloadsThePreviousBackend() {
        val first = FakeSpeechBackend("first")
        val second = FakeSpeechBackend("second")
        val engine = RoutedSpeechToTextEngine(setOf(first, second))

        engine.load(requestFor("first"))
        engine.load(requestFor("second"))

        assertEquals(listOf("load", "cancel", "unload"), first.events)
        assertTrue(second.isLoaded)
        assertFalse(first.isLoaded)
    }

    @Test(expected = IllegalStateException::class)
    fun transcriptionBeforeLoadFails() {
        RoutedSpeechToTextEngine(setOf(FakeSpeechBackend("only"))).transcribe(
            SpeechToTextRequest(floatArrayOf(0f), 16_000),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateBackendIdsAreRejected() {
        RoutedSpeechToTextEngine(setOf(FakeSpeechBackend("duplicate"), FakeSpeechBackend("duplicate")))
    }

    private fun requestFor(id: String) = SpeechToTextLoadRequest(
        engineId = EngineId(id),
        profileType = ModelProfileId.WHISPER_STT,
        modelDirectory = "model",
        files = emptyMap(),
        languageCode = "en",
    )

    private class FakeSpeechBackend(id: String) : SpeechToTextBackend {
        override val engineId = EngineId(id)
        override var isLoaded = false
        val events = mutableListOf<String>()

        override fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult {
            events += "load"
            isLoaded = true
            return SpeechToTextLoadResult(1, 1, coldStart = true)
        }

        override fun transcribe(request: SpeechToTextRequest) = SpeechToTextResult("ok", 1)

        override fun cancel() {
            events += "cancel"
        }

        override fun unload() {
            events += "unload"
            isLoaded = false
        }
    }
}
