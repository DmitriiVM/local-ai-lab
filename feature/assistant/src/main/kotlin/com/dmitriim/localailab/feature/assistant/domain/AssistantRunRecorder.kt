package com.dmitriim.localailab.feature.assistant.domain

import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.feature.runs.api.domain.history.RunModelSnapshot
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import com.dmitriim.localailab.feature.runs.api.data.RunRepository
import com.dmitriim.localailab.core.voice.stt.SpeechTranscriptionMetrics
import com.dmitriim.localailab.core.voice.tts.SpeechSynthesisMetrics
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Inject
class AssistantRunRecorder(
    private val runRepository: RunRepository,
) {
    suspend fun recordSpeechInput(
        status: RunStatus,
        startedAtEpochMs: Long,
        model: RunModelSnapshot?,
        transcript: String?,
        languageCode: String,
        threadCount: Int,
        metrics: SpeechTranscriptionMetrics?,
        error: String?,
    ): String = save(
        capability = AiCapability.SPEECH_TO_TEXT,
        status = status,
        startedAtEpochMs = startedAtEpochMs,
        model = model,
        input = "Microphone recording",
        output = transcript,
        parameters = buildJsonObject {
            put("language", languageCode)
            put("threadCount", threadCount)
        },
        metrics = buildJsonObject {
            metrics?.let {
                put("audioDurationMs", it.audioDurationMs)
                put("processingDurationMs", it.processingDurationMs)
                put("timeToFinalMs", it.timeToFinalMs)
                put("segmentCount", it.segmentCount)
            }
        },
        error = error,
    )

    suspend fun recordSpeechOutput(
        status: RunStatus,
        startedAtEpochMs: Long,
        model: RunModelSnapshot?,
        text: String,
        languageCode: String,
        voiceId: String,
        metrics: SpeechSynthesisMetrics?,
        error: String?,
    ): String = save(
        capability = AiCapability.TEXT_TO_SPEECH,
        status = status,
        startedAtEpochMs = startedAtEpochMs,
        model = model,
        input = text,
        output = metrics?.let { "Generated speech" },
        parameters = buildJsonObject {
            put("language", languageCode)
            put("voiceId", voiceId)
        },
        metrics = buildJsonObject {
            metrics?.let {
                put("synthesisDurationMs", it.synthesisDurationMs)
                put("generatedAudioDurationMs", it.generatedAudioDurationMs)
                put("timeToFirstPresentationMs", it.timeToFirstPresentationMs)
            }
        },
        error = error,
    )

    suspend fun recordVoiceTurn(
        status: RunStatus,
        startedAtEpochMs: Long,
        transcript: String?,
        response: String?,
        linkedRunIds: List<String>,
        error: String?,
    ): String = save(
        capability = AiCapability.VOICE_ASSISTANT,
        status = status,
        startedAtEpochMs = startedAtEpochMs,
        model = null,
        input = transcript,
        output = response,
        parameters = buildJsonObject { put("inputMode", "VOICE") },
        metrics = buildJsonObject {},
        error = error,
        linkedRunIds = linkedRunIds,
    )

    private suspend fun save(
        capability: AiCapability,
        status: RunStatus,
        startedAtEpochMs: Long,
        model: RunModelSnapshot?,
        input: String?,
        output: String?,
        parameters: kotlinx.serialization.json.JsonObject,
        metrics: kotlinx.serialization.json.JsonObject,
        error: String?,
        linkedRunIds: List<String> = emptyList(),
    ): String {
        val id = UUID.randomUUID().toString()
        runRepository.saveRun(
            RunRecord(
                id = id,
                capability = capability,
                status = status,
                startedAtEpochMs = startedAtEpochMs,
                completedAtEpochMs = System.currentTimeMillis(),
                model = model,
                input = input,
                output = output,
                parametersJson = Json.encodeToString(parameters),
                metricsJson = Json.encodeToString(metrics),
                errorMessage = error,
                linkedRunIds = linkedRunIds,
            ),
        )
        return id
    }
}
