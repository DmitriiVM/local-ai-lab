package com.dmitriim.localaiplayground.ai.litertlm

import com.dmitriim.localaiplayground.ai.api.llm.LlmChatMessage
import com.dmitriim.localaiplayground.ai.api.llm.LlmChatRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Carries structured chat history through the engine-neutral string prompt boundary. */
internal object LiteRtLmPromptCodec {
    private const val VERSION = 1
    private val json = Json { encodeDefaults = true }

    fun encode(messages: List<LlmChatMessage>): String {
        require(messages.isNotEmpty()) { "A chat prompt needs at least one message." }
        return json.encodeToString(
            PromptEnvelope(
                version = VERSION,
                messages = messages.map { message ->
                    PromptMessage(role = message.role.wireName, content = message.content)
                },
            ),
        )
    }

    fun decode(prompt: String): List<LlmChatMessage> {
        val envelope = json.decodeFromString<PromptEnvelope>(prompt)
        require(envelope.version == VERSION) { "Unsupported LiteRT-LM prompt envelope version." }
        return envelope.messages.map { message ->
            LlmChatMessage(
                role = when (message.role) {
                    LlmChatRole.SYSTEM.wireName -> LlmChatRole.SYSTEM
                    LlmChatRole.USER.wireName -> LlmChatRole.USER
                    LlmChatRole.ASSISTANT.wireName -> LlmChatRole.ASSISTANT
                    else -> error("Unsupported chat role: ${message.role}")
                },
                content = message.content,
            )
        }
    }

    @Serializable
    private data class PromptEnvelope(val version: Int, val messages: List<PromptMessage>)

    @Serializable
    private data class PromptMessage(val role: String, val content: String)
}
