package com.dmitriim.localailab.ai.litertlm

import com.dmitriim.localailab.ai.api.chat.LlmChatMessage
import com.dmitriim.localailab.ai.api.chat.LlmChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LiteRtLmPromptCodecTest {
    @Test
    fun encodesStablePromptEnvelope() {
        val prompt = LiteRtLmPromptCodec.encode(
            listOf(
                LlmChatMessage(LlmChatRole.SYSTEM, "Be concise."),
                LlmChatMessage(LlmChatRole.USER, "Hello"),
            ),
        )

        assertEquals(
            """{"version":1,"messages":[{"role":"system","content":"Be concise."},{"role":"user","content":"Hello"}]}""",
            prompt,
        )
    }

    @Test
    fun decodesKnownPromptEnvelope() {
        val messages = LiteRtLmPromptCodec.decode(
            """{"version":1,"messages":[{"role":"assistant","content":"Hi"}]}""",
        )

        assertEquals(listOf(LlmChatMessage(LlmChatRole.ASSISTANT, "Hi")), messages)
    }

    @Test
    fun rejectsUnknownChatRoles() {
        assertThrows(IllegalStateException::class.java) {
            LiteRtLmPromptCodec.decode(
                """{"version":1,"messages":[{"role":"tool","content":"ignored"}]}""",
            )
        }
    }
}
