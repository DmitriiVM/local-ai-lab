package com.dmitriim.localailab.ai.api.llm

import com.dmitriim.localailab.core.model.engine.ComputePreference
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ChatModelReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutedChatEngineTest {
    @Test
    fun switchingModelsCancelsAndUnloadsThePreviousRuntime() {
        val first = FakeChatRuntime("first")
        val second = FakeChatRuntime("second")
        val engine = RoutedChatEngine(setOf(first, second))

        engine.load(requestFor("first"))
        engine.load(requestFor("second"))

        assertEquals(listOf("load", "cancel", "unload"), first.events)
        assertEquals(listOf("load"), second.events)
        assertFalse(first.isLoaded)
        assertTrue(second.isLoaded)
    }

    @Test
    fun loadingTheSameRuntimeDoesNotRestartIt() {
        val runtime = FakeChatRuntime("only")
        val engine = RoutedChatEngine(setOf(runtime))

        engine.load(requestFor("only"))
        engine.load(requestFor("only"))

        assertEquals(listOf("load", "load"), runtime.events)
    }

    @Test
    fun unloadClearsActiveRuntimeAndLoadedState() {
        val runtime = FakeChatRuntime("only")
        val engine = RoutedChatEngine(setOf(runtime))
        engine.load(requestFor("only"))

        engine.unload()

        assertFalse(engine.isLoaded)
        assertEquals(listOf("load", "cancel", "unload"), runtime.events)
    }

    @Test(expected = IllegalStateException::class)
    fun generationBeforeLoadFails() {
        RoutedChatEngine(setOf(FakeChatRuntime("only"))).generate(
            LlmGenerationRequest("prompt"),
            onToken = {},
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateEngineIdsAreRejected() {
        RoutedChatEngine(setOf(FakeChatRuntime("duplicate"), FakeChatRuntime("duplicate")))
    }

    private fun requestFor(engineId: String) = LlmLoadRequest(
        model = ChatModelReference.SystemManaged(
            modelId = ModelId("model-$engineId"),
            displayName = engineId,
            engineId = EngineId(engineId),
            profileType = ModelProfileIds.LLM,
            defaultContextSize = 512,
        ),
        options = LlmLoadOptions(computePreference = ComputePreference.AUTO),
    )

    private class FakeChatRuntime(id: String) : ChatRuntime {
        override val engineId = EngineId(id)
        override val capabilities = LlmEngineCapabilities(
            computePreferences = setOf(ComputePreference.AUTO),
            streaming = true,
            cancellation = true,
            tokenCounting = false,
            chatTemplateHandling = LlmChatTemplateHandling.CALLER_PROVIDES_PROMPT,
            systemInstructions = false,
            contextManagement = LlmContextManagement.RUNTIME_MANAGED,
            loadOptions = emptySet(),
            generationOptions = emptySet(),
        )
        override var isLoaded = false
        val events = mutableListOf<String>()

        override fun load(request: LlmLoadRequest): LlmLoadResult {
            events += "load"
            isLoaded = true
            return LlmLoadResult(ComputePreference.AUTO, 1L, coldStart = true)
        }

        override fun generate(
            request: LlmGenerationRequest,
            onToken: (String) -> Unit,
        ) = LlmGenerationResult(
            text = "ok",
            promptTokenCount = null,
            generatedTokenCount = null,
            firstTokenLatencyMs = null,
            promptDurationMs = 0,
            generationDurationMs = 0,
            totalDurationMs = 0,
            finishReason = LlmFinishReason.STOP_TOKEN,
        )

        override fun cancel() {
            events += "cancel"
        }

        override fun unload() {
            events += "unload"
            isLoaded = false
        }
    }
}
