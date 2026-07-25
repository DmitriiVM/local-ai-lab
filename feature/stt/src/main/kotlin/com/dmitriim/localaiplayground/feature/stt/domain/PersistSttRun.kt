package com.dmitriim.localaiplayground.feature.stt.domain

import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.RunRecord
import com.dmitriim.localaiplayground.core.model.RunRepository
import com.dmitriim.localaiplayground.core.model.RunStatus
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Inject
class PersistSttRun(private val runRepository: RunRepository) {
    suspend operator fun invoke(snapshot: SttRunSnapshot) {
        runRepository.saveRun(
            RunRecord(
                id = UUID.randomUUID().toString(),
                capability = AiCapability.SPEECH_TO_TEXT,
                status = snapshot.status,
                startedAtEpochMs = snapshot.startedAtEpochMs,
                completedAtEpochMs = System.currentTimeMillis(),
                model = snapshot.model,
                input = snapshot.inputDescription,
                output = snapshot.transcript,
                parametersJson = Json.encodeToString(buildJsonObject {
                    put("language", snapshot.languageCode)
                    put("threadCount", snapshot.threadCount)
                }),
                metricsJson = snapshot.metrics?.let { metrics ->
                    Json.encodeToString(buildJsonObject {
                        put("audioDurationMs", metrics.audioDurationMs)
                        put("processingDurationMs", metrics.processingDurationMs)
                        put("timeToFinalMs", metrics.timeToFinalMs)
                        put("segmentCount", metrics.segmentCount)
                        put("loadDurationMs", metrics.loadDurationMs)
                        put("effectiveThreadCount", metrics.effectiveThreadCount)
                    })
                } ?: "{}",
                errorMessage = snapshot.errorMessage,
            ),
        )
    }
}

data class SttRunSnapshot(
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
