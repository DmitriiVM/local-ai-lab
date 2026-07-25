package com.dmitriim.localaiplayground.feature.chat.domain

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.ChatEngine
import com.dmitriim.localaiplayground.ai.api.LlmChatMessage
import com.dmitriim.localaiplayground.ai.api.LlmChatRole

/** Enforces the documented oldest-turn truncation strategy before a local generation. */
internal class ChatPromptPreparer(
    private val chatEngine: ChatEngine,
) {
    fun prepare(turns: List<ChatTurn>, config: ChatGenerationConfig): PreparedChatPrompt {
        var included = turns
        var omitted = 0
        Log.i(TAG, "Chat prompt preparation started: turns=${turns.size}, systemPromptChars=${config.systemPrompt.length}")
        while (true) {
            val prompt = chatEngine.format(
                buildList {
                    if (config.systemPrompt.isNotBlank()) add(LlmChatMessage(LlmChatRole.SYSTEM, config.systemPrompt))
                    addAll(included.map { turn -> LlmChatMessage(turn.role.toEngineRole(), turn.content) })
                },
            )
            val tokenCount = chatEngine.countTokens(prompt)
            if (tokenCount + config.maxOutputTokens <= config.contextSize) {
                Log.i(TAG, "Chat prompt fits context: promptTokens=$tokenCount, reservedOutputTokens=${config.maxOutputTokens}, omittedMessages=$omitted")
                return PreparedChatPrompt(
                    prompt = prompt,
                    usage = ChatContextUsage(tokenCount, config.contextSize, config.maxOutputTokens, omitted),
                )
            }
            val firstUser = included.indexOfFirst { it.role == ChatTurnRole.USER }
            val latestUser = included.indexOfLast { it.role == ChatTurnRole.USER }
            require(firstUser >= 0 && firstUser != latestUser) {
                "The system prompt and latest user message do not fit with the requested output in this context. Increase context size or reduce maximum output tokens."
            }
            val removeThrough = if (included.getOrNull(firstUser + 1)?.role == ChatTurnRole.ASSISTANT) firstUser + 1 else firstUser
            omitted += removeThrough + 1
            Log.i(TAG, "Chat prompt truncating oldest messages: removing=${removeThrough + 1}, totalOmitted=$omitted, promptTokens=$tokenCount")
            included = included.drop(removeThrough + 1)
        }
    }
}

private const val TAG = "AiP123Chat"

internal data class PreparedChatPrompt(val prompt: String, val usage: ChatContextUsage)

private fun ChatTurnRole.toEngineRole() = when (this) {
    ChatTurnRole.USER -> LlmChatRole.USER
    ChatTurnRole.ASSISTANT -> LlmChatRole.ASSISTANT
}
