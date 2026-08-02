package com.dmitriim.localaiplayground.feature.tts.domain

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.service.RunRepository
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Inject
class PersistTtsRun(private val runRepository: RunRepository) {
    suspend operator fun invoke(snapshot: TtsRunSnapshot) {
        runRepository.saveRun(
            RunRecord(
                id = UUID.randomUUID().toString(),
                capability = AiCapability.TEXT_TO_SPEECH,
                status = snapshot.status,
                startedAtEpochMs = snapshot.startedAtEpochMs,
                completedAtEpochMs = System.currentTimeMillis(),
                model = snapshot.model,
                input = snapshot.input,
                output = snapshot.metrics?.let { "Generated ${it.generatedAudioDurationMs} ms WAV at ${it.sampleRateHz} Hz." },
                parametersJson = Json.encodeToString(
                    buildJsonObject {
                        put("language", snapshot.languageCode)
                        put("voiceId", snapshot.voiceId)
                        put("voiceName", snapshot.voiceName)
                        put("speakerId", snapshot.speakerId)
                        put("referenceVoiceId", snapshot.referenceVoiceId)
                        put("referenceVoiceName", snapshot.referenceVoiceName)
                        put("watermarkStatus", snapshot.watermarkStatus)
                        put("speed", snapshot.speed)
                        put("sentenceSilenceScale", snapshot.sentenceSilenceScale)
                        put("volume", snapshot.volume)
                        put("threadCount", snapshot.threadCount)
                        put("pitchSemitones", snapshot.audioEffects.pitchSemitones)
                        put("formantSemitones", snapshot.audioEffects.formantSemitones)
                        put("lowEqDb", snapshot.audioEffects.lowEqDb)
                        put("midEqDb", snapshot.audioEffects.midEqDb)
                        put("highEqDb", snapshot.audioEffects.highEqDb)
                        put("saturationDriveDb", snapshot.audioEffects.saturationDriveDb)
                    },
                ),
                metricsJson = snapshot.metrics?.let { metrics ->
                    Json.encodeToString(
                        buildJsonObject {
                            put("timeToFirstChunkMs", metrics.timeToFirstChunkMs)
                            put("timeToFirstWriteMs", metrics.timeToFirstWriteMs)
                            put("timeToFirstPresentationMs", metrics.timeToFirstPresentationMs)
                            put("synthesisDurationMs", metrics.synthesisDurationMs)
                            put("generatedAudioDurationMs", metrics.generatedAudioDurationMs)
                            put("sampleRateHz", metrics.sampleRateHz)
                            put("playbackUnderrunCount", metrics.playbackUnderrunCount)
                            put("effectiveThreadCount", metrics.effectiveThreadCount)
                            put("conditioningDurationMs", metrics.conditioningDurationMs)
                            put("tokenGenerationDurationMs", metrics.tokenGenerationDurationMs)
                            put("decoderDurationMs", metrics.decoderDurationMs)
                            put("generatedTokenCount", metrics.generatedTokenCount)
                            put("conditioningCacheHit", metrics.conditioningCacheHit)
                            put("peakProcessPssBytes", metrics.peakProcessPssBytes)
                            put("availableDeviceMemoryBytes", metrics.availableDeviceMemoryBytes)
                        },
                    )
                } ?: "{}",
                errorMessage = snapshot.errorMessage,
            ),
        )
    }
}
