package com.dmitriim.localailab.feature.assistant.domain

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPromptPreparerTest {
    @Test
    fun exactBudgetDropsOldestCompleteTurnPair() {
        val preparer = ChatPromptPreparer(
            chatEngine = FakeChatEngine(tokenCounter = LlmTokenCounter { it.length }),
            callerFormatter = CallerProvidedChatPromptFormatter { messages ->
                messages.joinToString(separator = "") { it.content }
            },
        )

        val result = preparer.prepare(
            turns = listOf(
                ChatTurn(ChatTurnRole.USER, "old"),
                ChatTurn(ChatTurnRole.ASSISTANT, "reply"),
                ChatTurn(ChatTurnRole.USER, "new"),
            ),
            config = config(contextSize = 9, maxOutputTokens = 2, systemPrompt = "S"),
            capabilities = callerBudgetCapabilities(LlmContextManagement.EXACT_CALLER_BUDGET),
        )

        assertEquals("Snew", result.prompt)
        assertEquals(2, result.usage.omittedTurnCount)
        assertEquals(4, result.usage.promptTokens)
        assertFalse(result.usage.promptTokensEstimated)
    }

    @Test
    fun boundaryPromptThatFitsIsRetained() {
        val preparer = ChatPromptPreparer(
            chatEngine = FakeChatEngine(tokenCounter = LlmTokenCounter { it.length }),
            callerFormatter = CallerProvidedChatPromptFormatter { messages -> messages.joinToString("") { it.content } },
        )

        val result = preparer.prepare(
            turns = listOf(ChatTurn(ChatTurnRole.USER, "hello")),
            config = config(contextSize = 7, maxOutputTokens = 2),
            capabilities = callerBudgetCapabilities(LlmContextManagement.EXACT_CALLER_BUDGET),
        )

        assertEquals("hello", result.prompt)
        assertEquals(0, result.usage.omittedTurnCount)
    }

    @Test
    fun runtimeManagedContextDoesNotTruncateHistory() {
        val preparer = ChatPromptPreparer(
            chatEngine = FakeChatEngine(),
            callerFormatter = CallerProvidedChatPromptFormatter { messages -> messages.joinToString("") { it.content } },
        )

        val result = preparer.prepare(
            turns = listOf(ChatTurn(ChatTurnRole.USER, "old"), ChatTurn(ChatTurnRole.USER, "new")),
            config = config(contextSize = 1, maxOutputTokens = 1),
            capabilities = LlmEngineCapabilities(
                computePreferences = setOf(ComputePreference.AUTO),
                streaming = true,
                cancellation = true,
                tokenCounting = false,
                chatTemplateHandling = LlmChatTemplateHandling.CALLER_PROVIDES_PROMPT,
                systemInstructions = true,
                contextManagement = LlmContextManagement.RUNTIME_MANAGED,
                loadOptions = emptySet(),
                generationOptions = emptySet(),
            ),
        )

        assertEquals("oldnew", result.prompt)
        assertEquals(0, result.usage.omittedTurnCount)
        assertEquals(null, result.usage.promptTokens)
    }

    @Test
    fun engineFormatterIsUsedWhenRuntimeOwnsMessageFormatting() {
        val preparer = ChatPromptPreparer(
            chatEngine = FakeChatEngine(formatter = LlmChatFormatter { messages -> "formatted:${messages.size}" }),
        )

        val result = preparer.prepare(
            turns = listOf(ChatTurn(ChatTurnRole.USER, "hello")),
            config = config(contextSize = 100, maxOutputTokens = 10, systemPrompt = "system"),
            capabilities = LlmEngineCapabilities(
                computePreferences = setOf(ComputePreference.AUTO),
                streaming = true,
                cancellation = true,
                tokenCounting = false,
                chatTemplateHandling = LlmChatTemplateHandling.ENGINE_FORMATS_MESSAGES,
                systemInstructions = true,
                contextManagement = LlmContextManagement.RUNTIME_MANAGED,
                loadOptions = emptySet(),
                generationOptions = emptySet(),
            ),
        )

        assertEquals("formatted:2", result.prompt)
        assertTrue(result.usage.contextManagement == LlmContextManagement.RUNTIME_MANAGED)
    }

    @Test(expected = IllegalArgumentException::class)
    fun latestUserMessageThatCannotFitFailsClearly() {
        ChatPromptPreparer(
            chatEngine = FakeChatEngine(tokenCounter = LlmTokenCounter { it.length }),
            callerFormatter = CallerProvidedChatPromptFormatter { messages -> messages.joinToString("") { it.content } },
        ).prepare(
            turns = listOf(ChatTurn(ChatTurnRole.USER, "too-long")),
            config = config(contextSize = 3, maxOutputTokens = 1),
            capabilities = callerBudgetCapabilities(LlmContextManagement.EXACT_CALLER_BUDGET),
        )
    }

    private fun callerBudgetCapabilities(management: LlmContextManagement) = LlmEngineCapabilities(
        computePreferences = setOf(ComputePreference.AUTO),
        streaming = true,
        cancellation = true,
        tokenCounting = true,
        chatTemplateHandling = LlmChatTemplateHandling.CALLER_PROVIDES_PROMPT,
        systemInstructions = true,
        contextManagement = management,
        loadOptions = setOf(LlmLoadOption.CONTEXT_SIZE),
        generationOptions = setOf(LlmGenerationOption.MAX_OUTPUT_TOKENS),
    )

    private fun config(contextSize: Int, maxOutputTokens: Int, systemPrompt: String = "") = ChatGenerationConfig(
        computePreference = ComputePreference.AUTO,
        systemPrompt = systemPrompt,
        temperature = 0.7f,
        topK = 40,
        topP = 0.9f,
        maxOutputTokens = maxOutputTokens,
        seed = null,
        contextSize = contextSize,
        threadCount = 1,
    )

    private class FakeChatEngine(
        private val formatter: LlmChatFormatter? = null,
        private val tokenCounter: LlmTokenCounter? = null,
    ) : ChatEngine {
        override val isLoaded = true
        override fun capabilitiesFor(engineId: EngineId): LlmEngineCapabilities? = null
        override fun activeChatFormatter() = formatter
        override fun activeTokenCounter() = tokenCounter
        override fun load(request: LlmLoadRequest) = LlmLoadResult(
            effectiveComputePreference = ComputePreference.AUTO,
            loadDurationMs = 0,
            coldStart = false,
            diagnostics = LlmRuntimeDiagnostics(),
        )
        override fun generate(request: LlmGenerationRequest, onToken: (String) -> Unit) = LlmGenerationResult(
            text = "",
            promptTokenCount = null,
            generatedTokenCount = null,
            firstTokenLatencyMs = null,
            promptDurationMs = 0,
            generationDurationMs = 0,
            totalDurationMs = 0,
            finishReason = LlmFinishReason.STOP_TOKEN,
        )
        override fun cancel() = Unit
        override fun unload() = Unit
    }
}
