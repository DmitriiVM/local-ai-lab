package com.dmitriim.localailab.feature.assistant.impl.domain

import com.dmitriim.localailab.ai.api.chat.ChatEngine
import com.dmitriim.localailab.ai.api.chat.LlmChatFormatter
import com.dmitriim.localailab.ai.api.chat.LlmChatTemplateHandling
import com.dmitriim.localailab.ai.api.chat.LlmContextManagement
import com.dmitriim.localailab.ai.api.chat.LlmEngineCapabilities
import com.dmitriim.localailab.ai.api.chat.LlmFinishReason
import com.dmitriim.localailab.ai.api.chat.LlmGenerationOption
import com.dmitriim.localailab.ai.api.chat.LlmGenerationRequest
import com.dmitriim.localailab.ai.api.chat.LlmGenerationResult
import com.dmitriim.localailab.ai.api.chat.LlmLoadOption
import com.dmitriim.localailab.ai.api.chat.LlmLoadRequest
import com.dmitriim.localailab.ai.api.chat.LlmLoadResult
import com.dmitriim.localailab.ai.api.chat.LlmRuntimeDiagnostics
import com.dmitriim.localailab.ai.api.chat.LlmTokenCounter
import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ChatModelReference
import com.dmitriim.localailab.feature.models.api.domain.runtime.SpeechToTextModelReference
import com.dmitriim.localailab.feature.models.api.domain.runtime.TextToSpeechModelReference
import com.dmitriim.localailab.feature.models.api.domain.runtime.LocalModelResolver
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenerateAssistantResponseTest {
    @Test
    fun `generation emits ordered events and forwards only supported options`() = runBlocking {
        val engine = FakeChatEngine()
        val generator = GenerateAssistantResponse(FakeModelResolver, engine)

        val events = generator.execute(request()).toList()

        assertEquals(
            listOf(
                ChatGenerationEvent.Prepared::class,
                ChatGenerationEvent.Token::class,
                ChatGenerationEvent.Token::class,
                ChatGenerationEvent.Completed::class,
            ),
            events.map { it::class },
        )
        assertEquals(8, engine.loadRequest?.options?.threadCount)
        assertNull(engine.loadRequest?.options?.contextSize)
        assertNull(engine.generationRequest?.options?.maxTokens)
        assertNull(engine.generationRequest?.options?.temperature)
        assertNull(engine.generationRequest?.options?.topK)
        assertEquals(0.8f, engine.generationRequest?.options?.topP)
        assertNull(engine.generationRequest?.options?.seed)
    }

    private fun request() = ChatGenerationRequest(
        modelId = ModelId("model"),
        turns = listOf(ChatTurn(ChatTurnRole.USER, "Hello")),
        config = ChatGenerationConfig(
            computePreference = ComputePreference.CPU,
            systemPrompt = "System",
            temperature = 0.7f,
            topK = 40,
            topP = 0.8f,
            maxOutputTokens = 128,
            seed = 4,
            contextSize = 4_096,
            threadCount = 8,
        ),
    )

    private object FakeModelResolver : LocalModelResolver {
        override suspend fun resolveChatModel(modelId: ModelId) = Result.success(
            ChatModelReference.SystemManaged(
                modelId = modelId,
                displayName = "Test model",
                engineId = EngineId("test"),
                profileType = ModelProfileId("LLM"),
                defaultContextSize = 2_048,
            ),
        )

        override suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference> = error("Not used by chat")

        override suspend fun resolveTextToSpeechModel(modelId: ModelId): Result<TextToSpeechModelReference> = error("Not used by chat")
    }

    private class FakeChatEngine : ChatEngine {
        var loadRequest: LlmLoadRequest? = null
        var generationRequest: LlmGenerationRequest? = null
        override val isLoaded = true

        override fun capabilitiesFor(engineId: EngineId) = LlmEngineCapabilities(
            computePreferences = setOf(ComputePreference.CPU),
            streaming = true,
            cancellation = true,
            tokenCounting = false,
            chatTemplateHandling = LlmChatTemplateHandling.ENGINE_FORMATS_MESSAGES,
            systemInstructions = true,
            contextManagement = LlmContextManagement.RUNTIME_MANAGED,
            loadOptions = setOf(LlmLoadOption.THREAD_COUNT),
            generationOptions = setOf(LlmGenerationOption.TOP_P),
        )

        override fun activeChatFormatter() = LlmChatFormatter { messages -> messages.joinToString("|") { it.content } }

        override fun activeTokenCounter(): LlmTokenCounter? = null

        override fun load(request: LlmLoadRequest): LlmLoadResult {
            loadRequest = request
            return LlmLoadResult(ComputePreference.CPU, 0, false, LlmRuntimeDiagnostics())
        }

        override fun generate(request: LlmGenerationRequest, onToken: (String) -> Unit): LlmGenerationResult {
            generationRequest = request
            onToken("Hi")
            onToken("!")
            return LlmGenerationResult(
                text = "Hi!",
                promptTokenCount = 2,
                generatedTokenCount = 2,
                firstTokenLatencyMs = 1,
                promptDurationMs = 1,
                generationDurationMs = 2,
                totalDurationMs = 3,
                finishReason = LlmFinishReason.STOP_TOKEN,
            )
        }

        override fun cancel() = Unit

        override fun unload() = Unit
    }
}
