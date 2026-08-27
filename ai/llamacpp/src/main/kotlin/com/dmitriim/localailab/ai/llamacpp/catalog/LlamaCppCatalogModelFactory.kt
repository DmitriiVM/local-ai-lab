package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.library.CatalogDownload
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.core.model.library.ModelCatalogState
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.ModelSource

internal fun llamaCppCatalogModel(
    modelId: String,
    displayName: String,
    family: String,
    description: String,
    repository: String,
    revision: String,
    fileName: String,
    quantization: String,
    expectedBytes: Long,
    sha256: String,
    languages: Set<String>,
    approximateRamBytes: Long,
    supportedLanguageCount: Int? = null,
    licenseName: String = "Apache-2.0",
    attribution: String = ModelCatalogDefaults.APACHE_ATTRIBUTION,
    state: ModelCatalogState = ModelCatalogState.OPTIONAL,
): CatalogModel {
    val downloadUrl = "https://huggingface.co/$repository/resolve/$revision/$fileName"
    val sourceUrl = "https://huggingface.co/$repository/tree/$revision"
    return CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId(modelId),
            displayName = displayName,
            family = family,
            description = description,
            capabilities = setOf(AiCapability.CHAT),
            engineId = EngineId("llama.cpp"),
            profileType = ModelProfileIds.LLM,
            format = ModelFormat.GGUF,
            quantization = quantization,
            architecture = family,
            revision = revision,
            files = listOf(
                ModelFileSpec(
                    relativePath = fileName,
                    role = ModelFileRoles.PRIMARY_MODEL,
                    expectedBytes = expectedBytes,
                    sha256 = sha256,
                ),
            ),
            source = ModelSource(
                url = sourceUrl,
                revision = revision,
                licenseName = licenseName,
                attribution = attribution,
            ),
            languages = languages,
            supportedLanguageCount = supportedLanguageCount,
            contextSize = 512,
            approximateRamBytes = approximateRamBytes,
            catalogVersion = ModelCatalogDefaults.VERSION,
            installedAtEpochMs = 0,
        ),
        state = state,
        download = CatalogDownload(
            url = downloadUrl,
            expectedBytes = expectedBytes,
            sha256 = sha256,
        ),
    )
}
