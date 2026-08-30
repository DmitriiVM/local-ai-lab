package com.dmitriim.localailab.ai.litertlm.catalog

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.api.model.library.CatalogDownload
import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadAuthentication
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogState
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.ai.api.model.manifest.ModelFormat
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelSource
import com.dmitriim.localailab.ai.litertlm.LiteRtLmRuntimeProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class Gemma3OneBLiteRtLmInt4ModelDefinition(
    override val runtimeProfile: LiteRtLmRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId("gemma-3-1b-litert-lm-int4"),
            displayName = "Gemma 3 1B LiteRT-LM INT4",
            family = "Gemma 3",
            description = "A compact Gemma 3 instruction model in LiteRT-LM format. A Hugging Face account and Gemma license acceptance are required before download.",
            capabilities = setOf(AiCapability.CHAT),
            engineId = runtimeProfile.key.engineId,
            profileType = runtimeProfile.key.profileId,
            format = ModelFormat.LITERT_LM,
            quantization = "INT4",
            architecture = "Gemma 3",
            revision = "6d54daa71cfbffba6b2843c08eeb1a27e7430bf0",
            files = listOf(
                ModelFileSpec(
                    relativePath = "gemma3-1b-it-int4.litertlm",
                    role = ModelFileRoles.PRIMARY_MODEL,
                    expectedBytes = 584_417_280L,
                    sha256 = "1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be",
                ),
            ),
            source = ModelSource(
                url = "https://huggingface.co/litert-community/Gemma3-1B-IT/tree/6d54daa71cfbffba6b2843c08eeb1a27e7430bf0",
                revision = "6d54daa71cfbffba6b2843c08eeb1a27e7430bf0",
                licenseName = "Gemma Terms of Use",
                attribution = ModelCatalogDefaults.GEMMA_ATTRIBUTION,
            ),
            languages = linkedSetOf("Multilingual"),
            contextSize = 4_096,
            approximateRamBytes = 3_000_000_000,
            catalogVersion = ModelCatalogDefaults.VERSION,
            installedAtEpochMs = 0,
        ),
        state = ModelCatalogState.OPTIONAL,
        download = CatalogDownload(
            url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/6d54daa71cfbffba6b2843c08eeb1a27e7430bf0/gemma3-1b-it-int4.litertlm",
            expectedBytes = 584_417_280L,
            sha256 = "1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be",
            authentication = CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN,
        ),
    )
}
