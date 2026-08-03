package com.dmitriim.localaiplayground.source.runs

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.conversation.ConversationKind
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRecord
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRole
import com.dmitriim.localaiplayground.core.model.conversation.ConversationRecord
import com.dmitriim.localaiplayground.core.model.runs.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.runs.RunStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RunDatabaseMappersTest {
    private val json = Json

    @Test
    fun `run round trip preserves optional fields and linked runs`() {
        val record = RunRecord(
            id = "run-1",
            capability = AiCapability.TEXT_TO_SPEECH,
            status = RunStatus.FAILED,
            startedAtEpochMs = 100L,
            completedAtEpochMs = 200L,
            model = RunModelSnapshot("model-1", "Test model", "engine", "r2"),
            input = "Hello",
            output = "Audio created",
            parametersJson = "{\"speed\":1.2}",
            metricsJson = "{\"durationMs\":20}",
            errorMessage = "playback failed",
            linkedRunIds = listOf("run-0", "run-2"),
        )

        assertEquals(record, record.toEntity(json).toDomain(json))
    }

    @Test
    fun `conversation and incomplete message round trip`() {
        val conversation = ConversationRecord(
            id = "conversation-1",
            kind = ConversationKind.ASSISTANT,
            title = "Assistant",
            createdAtEpochMs = 100L,
            updatedAtEpochMs = 200L,
        )
        val message = ConversationMessageRecord(
            id = "message-1",
            conversationId = conversation.id,
            role = ConversationMessageRole.ASSISTANT,
            content = "Partial response",
            createdAtEpochMs = 150L,
            incomplete = true,
        )

        assertEquals(conversation, conversation.toEntity().toDomain())
        assertEquals(message, message.toEntity().toDomain())
    }
}
