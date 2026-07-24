package com.dmitriim.localaiplayground.ai.sherpa

import java.io.File

/**
 * Typed file-role checks kept beside the sherpa-onnx adapter rather than in UI.
 * The Stage 0 harness uses these before it asks native code to parse a model.
 */
object SherpaProfiles {
    const val runtimeVersion = "1.13.4"
    const val runtimeSha256 = "03f9c4df965f21c71269365a7951a7f23b5696fddd093fa318c80d65550ab780"

    val whisperBaseRequiredFiles = setOf(
        "base-encoder.int8.onnx",
        "base-decoder.int8.onnx",
        "base-tokens.txt",
    )

    val supertonic3RequiredFiles = setOf(
        "duration_predictor.int8.onnx",
        "text_encoder.int8.onnx",
        "vector_estimator.int8.onnx",
        "vocoder.int8.onnx",
        "tts.json",
        "unicode_indexer.bin",
        "voice.bin",
    )

    fun missingFiles(modelDirectory: File, requiredFiles: Set<String>): Set<String> =
        requiredFiles.filterNot { File(modelDirectory, it).isFile }.toSet()
}
