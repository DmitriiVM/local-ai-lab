package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.stt.ParaformerSttProfile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class ParaformerSmallInt8ModelDefinition(
    override val runtimeProfile: ParaformerSttProfile,
) : ModelCatalogContribution {
    override val catalogModel = archiveSttCatalogModel(
        modelId = "paraformer-zh-en-small-int8",
        displayName = "Paraformer Small INT8",
        family = "Paraformer",
        description = "A Chinese-and-English offline speech-to-text model quantized to INT8.",
        profileKey = runtimeProfile.key,
        archiveName = "sherpa-onnx-paraformer-zh-small-2024-03-09",
        archiveBytes = 77_920_048,
        archiveSha256 = "da92b3db5218c5be53aad53e57d1b6e63e7fc98a0e054fbdd6dbe18e9c6b1450",
        files = listOf(
            ModelFileSpec("model.int8.onnx", ModelFileRoles.PRIMARY_MODEL, expectedBytes = 81_828_675),
            ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 75_352),
        ),
        languages = linkedSetOf("Chinese", "English"),
        licenseName = "Apache-2.0",
        attribution = "Paraformer model from ModelScope, converted and packaged for sherpa-onnx.",
        approximateRamBytes = 350_000_000,
    )
}
