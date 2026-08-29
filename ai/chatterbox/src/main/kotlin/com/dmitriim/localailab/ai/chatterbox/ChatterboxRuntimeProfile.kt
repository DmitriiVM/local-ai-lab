package com.dmitriim.localailab.ai.chatterbox

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRole
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceMode
import dev.zacsweers.metro.Inject
import java.io.File

internal interface ChatterboxProfile : ModelRuntimeProfile

internal object ChatterboxTtsArtifacts {
    val CONDITIONAL_DECODER = ModelFileRole("CONDITIONAL_DECODER")
    val EMBED_TOKENS = ModelFileRole("EMBED_TOKENS")
    val LANGUAGE_MODEL = ModelFileRole("LANGUAGE_MODEL")
    val SPEECH_ENCODER = ModelFileRole("SPEECH_ENCODER")
    val TOKENIZER = ModelFileRole("TOKENIZER")
}

private val chatterboxTtsProfileId = ModelProfileId("CHATTERBOX_TURBO_Q4")

@Inject
class ChatterboxRuntimeProfile : ChatterboxProfile {
    override val key = ModelProfileKey(EngineId("chatterbox-onnx"), chatterboxTtsProfileId)
    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.sampleRateHz == 24_000) { "Chatterbox Turbo output must be 24 kHz." }
        require(manifest.ttsVoiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
            "Chatterbox Turbo requires reference-audio voice metadata."
        }
        require(manifest.languages.any { it.equals("english", true) || it.equals("en", true) }) {
            "Chatterbox Turbo must declare English support."
        }
        val missing = requiredFiles.keys.filterNot { File(directory, it).isFile }
        require(missing.isEmpty()) { "Missing Chatterbox files: ${missing.joinToString()}" }
        requiredFiles.keys.filter { it.endsWith(".onnx") }.forEach { graph ->
            require(File(directory, "${graph}_data").isFile) { "$graph external data is missing." }
        }
        ChatterboxTokenizer(File(directory, "tokenizer.json"))
    }.fold(
        onSuccess = { RuntimeValidationResult(true) },
        onFailure = { RuntimeValidationResult(false, it.message) },
    )

    companion object {
        val requiredFiles = linkedMapOf(
            "conditional_decoder_q4.onnx" to ChatterboxTtsArtifacts.CONDITIONAL_DECODER,
            "conditional_decoder_q4.onnx_data" to ModelFileRoles.EXTERNAL_DATA,
            "embed_tokens_q4.onnx" to ChatterboxTtsArtifacts.EMBED_TOKENS,
            "embed_tokens_q4.onnx_data" to ModelFileRoles.EXTERNAL_DATA,
            "language_model_q4.onnx" to ChatterboxTtsArtifacts.LANGUAGE_MODEL,
            "language_model_q4.onnx_data" to ModelFileRoles.EXTERNAL_DATA,
            "speech_encoder_q4.onnx" to ChatterboxTtsArtifacts.SPEECH_ENCODER,
            "speech_encoder_q4.onnx_data" to ModelFileRoles.EXTERNAL_DATA,
            "tokenizer.json" to ChatterboxTtsArtifacts.TOKENIZER,
            "tokenizer_config.json" to ModelFileRoles.CONFIG,
            "config.json" to ModelFileRoles.CONFIG,
        )
    }
}
