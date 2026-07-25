package com.dmitriim.localaiplayground.ai.sherpa

import com.dmitriim.localaiplayground.ai.api.ModelRuntimeLoader
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import java.io.File

@Inject
@ContributesIntoSet(AppScope::class)
class SherpaModelRuntimeLoader : ModelRuntimeLoader {
    override val engineId = EngineId("sherpa-onnx")

    override fun load(manifest: ModelManifest, directory: File): AutoCloseable = when (manifest.profileType) {
        RuntimeProfileType.WHISPER_STT -> OfflineRecognizer(null, OfflineRecognizerConfig().apply {
            modelConfig = OfflineModelConfig().apply {
                whisper = OfflineWhisperModelConfig().apply {
                    encoder = File(directory, "base-encoder.int8.onnx").absolutePath
                    decoder = File(directory, "base-decoder.int8.onnx").absolutePath
                    language = "en"
                    task = "transcribe"
                }
                tokens = File(directory, "base-tokens.txt").absolutePath
                provider = "cpu"
            }
        }).let { recognizer -> AutoCloseable { recognizer.release() } }

        RuntimeProfileType.SILERO_VAD -> Vad(null, VadModelConfig().apply {
            sileroVadModelConfig = SileroVadModelConfig().apply {
                model = File(directory, "silero_vad.onnx").absolutePath
                windowSize = 512
            }
            sampleRate = 16_000
            provider = "cpu"
        }).let { vad -> AutoCloseable { vad.release() } }

        RuntimeProfileType.SUPERTONIC_TTS -> OfflineTts(null, OfflineTtsConfig().apply {
            model = OfflineTtsModelConfig().apply {
                supertonic = OfflineTtsSupertonicModelConfig().apply {
                    durationPredictor = File(directory, "duration_predictor.int8.onnx").absolutePath
                    textEncoder = File(directory, "text_encoder.int8.onnx").absolutePath
                    vectorEstimator = File(directory, "vector_estimator.int8.onnx").absolutePath
                    vocoder = File(directory, "vocoder.int8.onnx").absolutePath
                    ttsJson = File(directory, "tts.json").absolutePath
                    unicodeIndexer = File(directory, "unicode_indexer.bin").absolutePath
                    voiceStyle = File(directory, "voice.bin").absolutePath
                }
                provider = "cpu"
            }
        }).let { tts -> AutoCloseable { tts.release() } }

        else -> error("Unsupported sherpa-onnx profile: ${manifest.profileType}")
    }
}
