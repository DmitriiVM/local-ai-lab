package com.dmitriim.localailab.feature.assistant.domain

import android.util.Log
import com.dmitriim.localailab.ai.api.llm.ChatEngine
import com.dmitriim.localailab.ai.api.llm.LlmChatMessage
import com.dmitriim.localailab.ai.api.llm.LlmChatRole
import com.dmitriim.localailab.ai.api.llm.LlmChatTemplateHandling
import com.dmitriim.localailab.ai.api.llm.LlmContextManagement
import com.dmitriim.localailab.ai.api.llm.LlmEngineCapabilities
import com.dmitriim.localailab.ai.api.llm.LlmGenerationOption
import com.dmitriim.localailab.ai.api.llm.LlmLoadOption

/** Applies the selected runtime's prompt capabilities and exact budgeting when available. */
internal class ChatPromptPreparer(
    private val chatEngine: ChatEngine,
    private val callerFormatter: CallerProvidedChatPromptFormatter = RoleLabeledChatPromptFormatter,
    private val tokenEstimator: PromptTokenEstimator = ConservativeUtf8PromptTokenEstimator,
) {
    fun prepare(
        turns: List<ChatTurn>,
        config: ChatGenerationConfig,
        capabilities: LlmEngineCapabilities,
    ): PreparedChatPrompt {
        val formatter = promptFormatter(capabilities)
        val tokenCounter = activeTokenCounter(capabilities)
        val contextSize = config.contextSize.takeIf { LlmLoadOption.CONTEXT_SIZE in capabilities.loadOptions }
        val reservedOutputTokens = config.maxOutputTokens.takeIf {
            LlmGenerationOption.MAX_OUTPUT_TOKENS in capabilities.generationOptions
        }
        val promptBudget = promptBudget(capabilities, tokenCounter, contextSize, reservedOutputTokens)
        var included = turns
        var omitted = 0
        Log.i(
            TAG,
            "Chat prompt preparation started: turns=${turns.size}, systemInstructions=${capabilities.systemInstructions}, " +
                "templateHandling=${capabilities.chatTemplateHandling}, exactTokenCounting=${tokenCounter != null}",
        )
        if (promptBudget == null) {
            return runtimeManagedPrompt(formatter, included, config, capabilities, tokenCounter, contextSize, reservedOutputTokens)
        }
        while (true) {
            val prompt = formatter(buildMessages(included, config, capabilities.systemInstructions))
            val tokenCount = promptBudget.countTokens(prompt)
            if (tokenCount.toLong() + promptBudget.reservedOutputTokens <= promptBudget.contextSize.toLong()) {
                Log.i(
                    TAG,
                    "Chat prompt prepared: promptTokens=$tokenCount, estimated=${promptBudget.estimated}, " +
                        "contextSize=${promptBudget.contextSize}, " +
                        "reservedOutputTokens=${promptBudget.reservedOutputTokens}, omittedMessages=$omitted",
                )
                return PreparedChatPrompt(
                    prompt = prompt,
                    usage = ChatContextUsage(
                        promptTokens = tokenCount,
                        promptTokensEstimated = promptBudget.estimated,
                        contextSize = promptBudget.contextSize,
                        reservedOutputTokens = promptBudget.reservedOutputTokens,
                        omittedTurnCount = omitted,
                        contextManagement = capabilities.contextManagement,
                    ),
                )
            }
            val removed = included.removableTurnCount()
            omitted += removed
            Log.i(
                TAG,
                "Chat prompt truncating oldest messages: removing=$removed, " +
                    "totalOmitted=$omitted, promptTokens=$tokenCount",
            )
            included = included.drop(removed)
        }
    }

    private fun promptFormatter(capabilities: LlmEngineCapabilities): (List<LlmChatMessage>) -> String = when (capabilities.chatTemplateHandling) {
        LlmChatTemplateHandling.ENGINE_FORMATS_MESSAGES -> requireNotNull(chatEngine.activeChatFormatter()) {
            "The active LLM runtime does not provide its declared chat formatter."
        }::format
        LlmChatTemplateHandling.CALLER_PROVIDES_PROMPT -> callerFormatter::format
    }

    private fun activeTokenCounter(capabilities: LlmEngineCapabilities) = if (capabilities.tokenCounting) {
        requireNotNull(chatEngine.activeTokenCounter()) {
            "The active LLM runtime does not provide its declared token counter."
        }
    } else {
        null
    }

    private fun promptBudget(
        capabilities: LlmEngineCapabilities,
        tokenCounter: com.dmitriim.localailab.ai.api.llm.LlmTokenCounter?,
        contextSize: Int?,
        reservedOutputTokens: Int?,
    ): PromptBudget? = when (capabilities.contextManagement) {
        LlmContextManagement.EXACT_CALLER_BUDGET -> PromptBudget(
            countTokens = requireNotNull(tokenCounter) { "Exact caller context budgeting requires the active runtime's token counter." }::countTokens,
            estimated = false,
            contextSize = requireNotNull(contextSize) { "Exact caller context budgeting requires a context-size control." },
            reservedOutputTokens = requireNotNull(reservedOutputTokens) { "Exact caller context budgeting requires a maximum-output control." },
        )
        LlmContextManagement.ESTIMATED_CALLER_BUDGET -> PromptBudget(
            countTokens = tokenEstimator::estimate,
            estimated = true,
            contextSize = requireNotNull(contextSize) { "Estimated caller context budgeting requires a context-size control." },
            reservedOutputTokens = requireNotNull(reservedOutputTokens) { "Estimated caller context budgeting requires a maximum-output control." },
        )
        LlmContextManagement.RUNTIME_MANAGED -> null
    }

    private fun runtimeManagedPrompt(
        formatter: (List<LlmChatMessage>) -> String,
        turns: List<ChatTurn>,
        config: ChatGenerationConfig,
        capabilities: LlmEngineCapabilities,
        tokenCounter: com.dmitriim.localailab.ai.api.llm.LlmTokenCounter?,
        contextSize: Int?,
        reservedOutputTokens: Int?,
    ): PreparedChatPrompt {
        val prompt = formatter(buildMessages(turns, config, capabilities.systemInstructions))
        val tokenCount = tokenCounter?.countTokens(prompt)
        Log.i(TAG, "Chat prompt prepared with runtime-managed context: promptTokens=$tokenCount, omittedMessages=0")
        return PreparedChatPrompt(
            prompt = prompt,
            usage = ChatContextUsage(tokenCount, false, contextSize, reservedOutputTokens, 0, capabilities.contextManagement),
        )
    }

    private fun buildMessages(
        turns: List<ChatTurn>,
        config: ChatGenerationConfig,
        systemInstructions: Boolean,
    ) = buildList {
        if (systemInstructions && config.systemPrompt.isNotBlank()) {
            add(LlmChatMessage(LlmChatRole.SYSTEM, config.systemPrompt))
        }
        addAll(turns.map { turn -> LlmChatMessage(turn.role.toEngineRole(), turn.content) })
    }
}

private fun List<ChatTurn>.removableTurnCount(): Int {
    val firstUser = indexOfFirst { it.role == ChatTurnRole.USER }
    require(firstUser >= 0 && firstUser != indexOfLast { it.role == ChatTurnRole.USER }) {
        "The system prompt and latest user message do not fit with the requested output in this context. Increase context size or reduce maximum output tokens."
    }
    return firstUser + if (getOrNull(firstUser + 1)?.role == ChatTurnRole.ASSISTANT) 2 else 1
}

private const val TAG = "AiP123Chat"

internal data class PreparedChatPrompt(val prompt: String, val usage: ChatContextUsage)

private data class PromptBudget(
    val countTokens: (String) -> Int,
    val estimated: Boolean,
    val contextSize: Int,
    val reservedOutputTokens: Int,
)

private fun ChatTurnRole.toEngineRole() = when (this) {
    ChatTurnRole.USER -> LlmChatRole.USER
    ChatTurnRole.ASSISTANT -> LlmChatRole.ASSISTANT
}
