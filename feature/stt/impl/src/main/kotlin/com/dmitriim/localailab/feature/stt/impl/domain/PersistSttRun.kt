package com.dmitriim.localailab.feature.stt.impl.domain

import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.feature.runs.api.domain.history.RunModelSnapshot
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import com.dmitriim.localailab.feature.runs.api.data.RunRepository
import com.dmitriim.localailab.core.performance.profiling.serialization.putInferenceTelemetry
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionMetrics
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Inject
class PersistSttRun(private val runRepository: RunRepository) {
    suspend operator fun invoke(snapshot: SttRunSnapshot) {
        runRepository.saveRun(
            RunRecord(
                id = snapshot.runId,
                capability = AiCapability.SPEECH_TO_TEXT,
                status = snapshot.status,
                startedAtEpochMs = snapshot.startedAtEpochMs,
                completedAtEpochMs = System.currentTimeMillis(),
                model = snapshot.model,
                input = snapshot.inputDescription,
                output = snapshot.transcript,
                parametersJson = Json.encodeToString(
                    buildJsonObject {
                        put("language", snapshot.languageCode)
                        put("threadCount", snapshot.threadCount)
                    },
                ),
                metricsJson = snapshot.metrics?.let { metrics ->
                    Json.encodeToString(
                        buildJsonObject {
                            put("audioDurationMs", metrics.audioDurationMs)
                            put("processingDurationMs", metrics.processingDurationMs)
                            put("timeToFinalMs", metrics.timeToFinalMs)
                            put("realTimeFactor", metrics.realTimeFactor)
                            put("segmentCount", metrics.segmentCount)
                            put("loadDurationMs", metrics.loadDurationMs)
                            put("effectiveThreadCount", metrics.effectiveThreadCount)
                            putInferenceTelemetry(metrics.telemetry)
                        },
                    )
                } ?: "{}",
                errorMessage = snapshot.errorMessage,
            ),
        )
    }
}

data class SttRunSnapshot(
    val runId: String,
    val status: RunStatus,
    val startedAtEpochMs: Long,
    val model: RunModelSnapshot?,
    val inputDescription: String,
    val transcript: String?,
    val languageCode: String,
    val threadCount: String,
    val metrics: SpeechTranscriptionMetrics?,
    val errorMessage: String?,
)
