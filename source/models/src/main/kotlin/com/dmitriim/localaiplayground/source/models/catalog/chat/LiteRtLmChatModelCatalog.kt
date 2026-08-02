package com.dmitriim.localaiplayground.source.models.catalog.chat

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.library.CatalogDownload
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
    private const val REPOSITORY = "litert-community/Qwen3-0.6B"
    private const val REVISION = "dd97997951bb15a2a71f539ba17f604707c0b11a"
    private const val FILE_NAME = "qwen3_0_6b_mixed_int4.litertlm"
    private const val EXPECTED_BYTES = 497_664_000L
    private const val SHA256 = "b1baab462f6be49d70eada79d715c2c52cd9ece0cad00bddf6a2c097d23498e9"

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
                revision = REVISION,
                files = listOf(
                    ModelFileSpec(
                        relativePath = FILE_NAME,
                        role = ModelFileRoles.PRIMARY_MODEL,
                        expectedBytes = EXPECTED_BYTES,
                        sha256 = SHA256,
                    ),
                ),
                source = ModelSource(
                    url = "https://huggingface.co/$REPOSITORY/tree/$REVISION",
                    revision = REVISION,
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
                url = "https://huggingface.co/$REPOSITORY/resolve/$REVISION/$FILE_NAME",
                expectedBytes = EXPECTED_BYTES,
                sha256 = SHA256,
            ),
        ),
    )
}
