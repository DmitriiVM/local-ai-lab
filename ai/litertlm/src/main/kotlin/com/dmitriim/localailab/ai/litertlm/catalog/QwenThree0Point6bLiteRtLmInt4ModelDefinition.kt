package com.dmitriim.localailab.ai.litertlm.catalog

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.api.model.library.CatalogDownload
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

private const val qwenRepository = "litert-community/Qwen3-0.6B"
private const val qwenRevision = "dd97997951bb15a2a71f539ba17f604707c0b11a"
private const val qwenFileName = "qwen3_0_6b_mixed_int4.litertlm"

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class QwenThree0Point6bLiteRtLmInt4ModelDefinition(
    override val runtimeProfile: LiteRtLmRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId("qwen3-0.6b-litert-lm-int4"),
            displayName = "Qwen3 0.6B LiteRT-LM INT4",
            family = "Qwen3",
            description =
            "A compact Qwen3 chat model in LiteRT-LM format with mixed INT4 weights for CPU " +
                "or GPU inference.",
            capabilities = setOf(AiCapability.CHAT),
            engineId = runtimeProfile.key.engineId,
            profileType = runtimeProfile.key.profileId,
            format = ModelFormat.LITERT_LM,
            quantization = "Mixed INT4",
            architecture = "Qwen3",
            revision = qwenRevision,
            files = listOf(
                ModelFileSpec(
                    relativePath = qwenFileName,
                    role = ModelFileRoles.PRIMARY_MODEL,
                    expectedBytes = 497_664_000L,
                    sha256 = "b1baab462f6be49d70eada79d715c2c52cd9ece0cad00bddf6a2c097d23498e9",
                ),
            ),
            source = ModelSource(
                url = "https://huggingface.co/$qwenRepository/tree/$qwenRevision",
                revision = qwenRevision,
                licenseName = "Apache-2.0",
                attribution = ModelCatalogDefaults.APACHE_ATTRIBUTION,
            ),
            languages = linkedSetOf("English", "Chinese"),
            contextSize = 2_048,
            approximateRamBytes = 3_000_000_000,
            catalogVersion = ModelCatalogDefaults.VERSION,
            installedAtEpochMs = 0,
        ),
        state = ModelCatalogState.OPTIONAL,
        download = CatalogDownload(
            url = "https://huggingface.co/$qwenRepository/resolve/$qwenRevision/$qwenFileName",
            expectedBytes = 497_664_000L,
            sha256 = "b1baab462f6be49d70eada79d715c2c52cd9ece0cad00bddf6a2c097d23498e9",
        ),
    )
}
