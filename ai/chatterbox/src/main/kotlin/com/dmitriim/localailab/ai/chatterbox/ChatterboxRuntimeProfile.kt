package com.dmitriim.localailab.ai.chatterbox

import com.dmitriim.localailab.ai.api.model.ModelImportDefinition
import com.dmitriim.localailab.ai.api.model.ModelImportFileDefinition
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import com.dmitriim.localailab.core.model.manifest.TtsVoiceMode
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.File

internal interface ChatterboxProfile : ModelRuntimeProfile

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelRuntimeProfile>())
class ChatterboxRuntimeProfile : ChatterboxProfile {
    override val key = ModelProfileKey(EngineId("chatterbox-onnx"), ModelProfileIds.CHATTERBOX_TURBO_Q4)
    override val displayName = "Chatterbox Turbo Q4 (English)"
    override val capabilities = setOf(AiCapability.TEXT_TO_SPEECH)
    override val importDefinition = ModelImportDefinition(
        displayName = displayName,
        format = ModelFormat.ONNX,
        files = requiredFiles.map { (path, role) -> ModelImportFileDefinition(role, relativePath = path) },
    )

    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.engineId == key.engineId && manifest.profileType == key.profileId)
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
            "conditional_decoder_q4.onnx" to ModelFileRoles.CONDITIONAL_DECODER,
            "conditional_decoder_q4.onnx_data" to ModelFileRoles.EXTERNAL_DATA,
            "embed_tokens_q4.onnx" to ModelFileRoles.EMBED_TOKENS,
            "embed_tokens_q4.onnx_data" to ModelFileRoles.EXTERNAL_DATA,
            "language_model_q4.onnx" to ModelFileRoles.LANGUAGE_MODEL,
            "language_model_q4.onnx_data" to ModelFileRoles.EXTERNAL_DATA,
            "speech_encoder_q4.onnx" to ModelFileRoles.SPEECH_ENCODER,
            "speech_encoder_q4.onnx_data" to ModelFileRoles.EXTERNAL_DATA,
            "tokenizer.json" to ModelFileRoles.TOKENIZER,
            "tokenizer_config.json" to ModelFileRoles.CONFIG,
            "config.json" to ModelFileRoles.CONFIG,
        )
    }
}
