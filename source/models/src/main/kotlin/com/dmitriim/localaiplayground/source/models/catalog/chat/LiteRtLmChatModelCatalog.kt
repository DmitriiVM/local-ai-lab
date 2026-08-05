package com.dmitriim.localaiplayground.source.models.catalog.chat

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.library.CatalogDownload
import com.dmitriim.localaiplayground.core.model.library.CatalogDownloadAuthentication
import com.dmitriim.localaiplayground.core.model.library.CatalogModel
import com.dmitriim.localaiplayground.core.model.library.ModelCatalogState
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRoles
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.manifest.ModelFormat
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileIds
import com.dmitriim.localaiplayground.core.model.manifest.ModelSource
import com.dmitriim.localaiplayground.source.models.catalog.CatalogDefaults

internal object LiteRtLmChatModelCatalog {
    private const val QWEN_REPOSITORY = "litert-community/Qwen3-0.6B"
    private const val QWEN_REVISION = "dd97997951bb15a2a71f539ba17f604707c0b11a"
    private const val QWEN_FILE_NAME = "qwen3_0_6b_mixed_int4.litertlm"
    private const val QWEN_EXPECTED_BYTES = 497_664_000L
    private const val QWEN_SHA256 = "b1baab462f6be49d70eada79d715c2c52cd9ece0cad00bddf6a2c097d23498e9"
    private const val GEMMA_REPOSITORY = "litert-community/Gemma3-1B-IT"
    private const val GEMMA_REVISION = "6d54daa71cfbffba6b2843c08eeb1a27e7430bf0"
    private const val GEMMA_FILE_NAME = "gemma3-1b-it-int4.litertlm"
    private const val GEMMA_EXPECTED_BYTES = 584_417_280L
    private const val GEMMA_SHA256 = "1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be"

    val entries: List<CatalogModel> = listOf(
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("qwen3-0.6b-litert-lm-int4"),
                displayName = "Qwen3 0.6B LiteRT-LM INT4",
                family = "Qwen3",
                description = "A compact Qwen3 chat model in LiteRT-LM format with mixed INT4 weights for CPU or GPU inference.",
                capabilities = setOf(AiCapability.CHAT),
                engineId = EngineId("litert-lm"),
                profileType = ModelProfileIds.LLM,
                format = ModelFormat.LITERT_LM,
                quantization = "Mixed INT4",
                architecture = "Qwen3",
                revision = QWEN_REVISION,
                files = listOf(
                    ModelFileSpec(
                        relativePath = QWEN_FILE_NAME,
                        role = ModelFileRoles.PRIMARY_MODEL,
                        expectedBytes = QWEN_EXPECTED_BYTES,
                        sha256 = QWEN_SHA256,
                    ),
                ),
                source = ModelSource(
                    url = "https://huggingface.co/$QWEN_REPOSITORY/tree/$QWEN_REVISION",
                    revision = QWEN_REVISION,
                    licenseName = "Apache-2.0",
                    attribution = CatalogDefaults.APACHE_ATTRIBUTION,
                ),
                languages = linkedSetOf("English", "Chinese"),
                contextSize = 2_048,
                approximateRamBytes = 3_000_000_000,
                catalogVersion = CatalogDefaults.VERSION,
                installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.OPTIONAL,
            download = CatalogDownload(
                url = "https://huggingface.co/$QWEN_REPOSITORY/resolve/$QWEN_REVISION/$QWEN_FILE_NAME",
                expectedBytes = QWEN_EXPECTED_BYTES,
                sha256 = QWEN_SHA256,
            ),
        ),
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("gemma-3-1b-litert-lm-int4"),
                displayName = "Gemma 3 1B LiteRT-LM INT4",
                family = "Gemma 3",
                description = "A compact Gemma 3 instruction model in LiteRT-LM format. A Hugging Face account and Gemma license acceptance are required before download.",
                capabilities = setOf(AiCapability.CHAT),
                engineId = EngineId("litert-lm"),
                profileType = ModelProfileIds.LLM,
                format = ModelFormat.LITERT_LM,
                quantization = "INT4",
                architecture = "Gemma 3",
                revision = GEMMA_REVISION,
                files = listOf(
                    ModelFileSpec(
                        relativePath = GEMMA_FILE_NAME,
                        role = ModelFileRoles.PRIMARY_MODEL,
                        expectedBytes = GEMMA_EXPECTED_BYTES,
                        sha256 = GEMMA_SHA256,
                    ),
                ),
                source = ModelSource(
                    url = "https://huggingface.co/$GEMMA_REPOSITORY/tree/$GEMMA_REVISION",
                    revision = GEMMA_REVISION,
                    licenseName = "Gemma Terms of Use",
                    attribution = CatalogDefaults.GEMMA_ATTRIBUTION,
                ),
                languages = linkedSetOf("Multilingual"),
                contextSize = 4_096,
                approximateRamBytes = 3_000_000_000,
                catalogVersion = CatalogDefaults.VERSION,
                installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.OPTIONAL,
            download = CatalogDownload(
                url = "https://huggingface.co/$GEMMA_REPOSITORY/resolve/$GEMMA_REVISION/$GEMMA_FILE_NAME",
                expectedBytes = GEMMA_EXPECTED_BYTES,
                sha256 = GEMMA_SHA256,
                authentication = CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN,
            ),
        ),
    )
}
