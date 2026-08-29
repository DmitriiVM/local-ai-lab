package com.dmitriim.localailab.feature.assistant.domain

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.feature.runs.api.domain.conversation.ConversationMessageRecord
import com.dmitriim.localailab.feature.runs.api.domain.conversation.ConversationRecord
import com.dmitriim.localailab.feature.runs.api.domain.storage.StorageUsage
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import com.dmitriim.localailab.feature.runs.api.data.RunRepository
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantRunRecorderTest {
    @Test
    fun `speech input records parameters metrics and errors`() = runBlocking {
        val repository = FakeRunRepository()
        val recorder = AssistantRunRecorder(repository)

        recorder.recordSpeechInput(
            status = RunStatus.FAILED,
            startedAtEpochMs = 1L,
            model = null,
            transcript = "partial",
            languageCode = "en",
            threadCount = 3,
            metrics = SpeechTranscriptionMetrics(10, 5, 12, 1.2, 2, 4, 3),
            error = "decoder failed",
        )

        val record = repository.savedRuns.single()
        assertEquals(AiCapability.SPEECH_TO_TEXT, record.capability)
        assertEquals("Microphone recording", record.input)
        assertEquals("decoder failed", record.errorMessage)
        assertEquals("3", Json.parseToJsonElement(record.parametersJson).jsonObject.getValue("threadCount").jsonPrimitive.content)
        assertEquals("2", Json.parseToJsonElement(record.metricsJson).jsonObject.getValue("segmentCount").jsonPrimitive.content)
    }

    @Test
    fun `voice output records linked run IDs and null metrics safely`() = runBlocking {
        val repository = FakeRunRepository()
        val recorder = AssistantRunRecorder(repository)

        recorder.recordVoiceTurn(
            status = RunStatus.CANCELLED,
            startedAtEpochMs = 1L,
            transcript = "hello",
            response = null,
            linkedRunIds = listOf("stt-run", "tts-run"),
            error = null,
        )

        val record = repository.savedRuns.single()
        assertEquals(AiCapability.VOICE_ASSISTANT, record.capability)
        assertEquals(listOf("stt-run", "tts-run"), record.linkedRunIds)
        assertEquals("VOICE", Json.parseToJsonElement(record.parametersJson).jsonObject.getValue("inputMode").jsonPrimitive.content)
        assertEquals("{}", record.metricsJson)
        assertNull(record.errorMessage)
    }

    private class FakeRunRepository : RunRepository {
        val savedRuns = mutableListOf<RunRecord>()
        override val runs: Flow<List<RunRecord>> = emptyFlow()
        override val conversations: Flow<List<ConversationRecord>> = emptyFlow()

        override fun observeRun(id: String): Flow<RunRecord?> = emptyFlow()

        override fun observeMessages(conversationId: String): Flow<List<ConversationMessageRecord>> = emptyFlow()

        override suspend fun saveRun(record: RunRecord) {
            savedRuns += record
        }

        override suspend fun saveConversation(record: ConversationRecord, messages: List<ConversationMessageRecord>) = Unit

        override suspend fun deleteConversation(id: String) = Unit

        override suspend fun clearRuns() = Unit

        override suspend fun storageUsage() = StorageUsage(0, 0, 0, 0)
    }
}
