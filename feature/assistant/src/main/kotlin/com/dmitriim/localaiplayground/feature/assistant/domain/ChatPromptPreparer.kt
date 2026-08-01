package com.dmitriim.localaiplayground.feature.assistant.domain

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.llm.ChatEngine
import com.dmitriim.localaiplayground.ai.api.llm.LlmChatMessage
import com.dmitriim.localaiplayground.ai.api.llm.LlmChatRole
import com.dmitriim.localaiplayground.ai.api.llm.LlmChatTemplateHandling
import com.dmitriim.localaiplayground.ai.api.llm.LlmContextManagement
import com.dmitriim.localaiplayground.ai.api.llm.LlmEngineCapabilities
import com.dmitriim.localaiplayground.ai.api.llm.LlmGenerationOption
import com.dmitriim.localaiplayground.ai.api.llm.LlmLoadOption

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
        val formatter: (List<LlmChatMessage>) -> String = when (capabilities.chatTemplateHandling) {
            LlmChatTemplateHandling.ENGINE_FORMATS_MESSAGES -> {
                val runtimeFormatter = requireNotNull(chatEngine.activeChatFormatter()) {
                    "The active LLM runtime does not provide its declared chat formatter."
                }
                runtimeFormatter::format
            }
            LlmChatTemplateHandling.CALLER_PROVIDES_PROMPT -> callerFormatter::format
        }
        val tokenCounter = if (capabilities.tokenCounting) {
            requireNotNull(chatEngine.activeTokenCounter()) {
                "The active LLM runtime does not provide its declared token counter."
            }
        } else {
            null
        }
        val contextSize = config.contextSize.takeIf { LlmLoadOption.CONTEXT_SIZE in capabilities.loadOptions }
        val reservedOutputTokens = config.maxOutputTokens.takeIf {
            LlmGenerationOption.MAX_OUTPUT_TOKENS in capabilities.generationOptions
        }
        val promptBudget = when (capabilities.contextManagement) {
            LlmContextManagement.EXACT_CALLER_BUDGET -> PromptBudget(
                countTokens = requireNotNull(tokenCounter) {
                    "Exact caller context budgeting requires the active runtime's token counter."
                }::countTokens,
                estimated = false,
                contextSize = requireNotNull(contextSize) {
                    "Exact caller context budgeting requires a context-size control."
                },
                reservedOutputTokens = requireNotNull(reservedOutputTokens) {
                    "Exact caller context budgeting requires a maximum-output control."
                },
            )
            LlmContextManagement.ESTIMATED_CALLER_BUDGET -> PromptBudget(
                countTokens = tokenEstimator::estimate,
                estimated = true,
                contextSize = requireNotNull(contextSize) {
                    "Estimated caller context budgeting requires a context-size control."
                },
                reservedOutputTokens = requireNotNull(reservedOutputTokens) {
                    "Estimated caller context budgeting requires a maximum-output control."
                },
            )
            LlmContextManagement.RUNTIME_MANAGED -> null
        }
        var included = turns
        var omitted = 0
        Log.i(
            TAG,
            "Chat prompt preparation started: turns=${turns.size}, systemInstructions=${capabilities.systemInstructions}, " +
                "templateHandling=${capabilities.chatTemplateHandling}, exactTokenCounting=${tokenCounter != null}",
        )
        if (promptBudget == null) {
            val prompt = formatter(buildMessages(included, config, capabilities.systemInstructions))
            val tokenCount = tokenCounter?.countTokens(prompt)
            Log.i(
                TAG,
                "Chat prompt prepared with runtime-managed context: promptTokens=$tokenCount, omittedMessages=0",
            )
            return PreparedChatPrompt(
                prompt = prompt,
                usage = ChatContextUsage(
                    promptTokens = tokenCount,
                    promptTokensEstimated = false,
                    contextSize = contextSize,
                    reservedOutputTokens = reservedOutputTokens,
                    omittedTurnCount = 0,
                    contextManagement = capabilities.contextManagement,
                ),
            )
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
            val firstUser = included.indexOfFirst { it.role == ChatTurnRole.USER }
            val latestUser = included.indexOfLast { it.role == ChatTurnRole.USER }
            require(firstUser >= 0 && firstUser != latestUser) {
                "The system prompt and latest user message do not fit with the requested output in this context. Increase context size or reduce maximum output tokens."
            }
            val removeThrough = if (included.getOrNull(firstUser + 1)?.role == ChatTurnRole.ASSISTANT) {
                firstUser + 1
            } else {
                firstUser
            }
            omitted += removeThrough + 1
            Log.i(
                TAG,
                "Chat prompt truncating oldest messages: removing=${removeThrough + 1}, " +
                    "totalOmitted=$omitted, promptTokens=$tokenCount",
            )
            included = included.drop(removeThrough + 1)
        }
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
