package com.dmitriim.localailab.ai.sherpa.catalog.stt

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.ai.sherpa.stt.profiles.GigaAmCtcSttProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class GigaAmV2CtcInt8ModelDefinition(
    override val runtimeProfile: GigaAmCtcSttProfile,
) : ModelCatalogContribution {
    override val catalogModel = archiveSttCatalogModel(
        modelId = "gigaam-v2-ctc-ru-int8",
        displayName = "GigaAM v2 CTC INT8",
        family = "GigaAM",
        description = "A Russian offline speech-to-text model quantized to INT8.",
        profileKey = runtimeProfile.key,
        archiveName = "sherpa-onnx-nemo-ctc-giga-am-v2-russian-2025-04-19",
        archiveBytes = 166_917_722,
        archiveSha256 = "777be8717d8aaf04861823671290f7687f7579fd9ac63a2124955573f920caf5",
        files = listOf(
            ModelFileSpec("model.int8.onnx", ModelFileRoles.PRIMARY_MODEL, expectedBytes = 236_457_977),
            ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 196),
        ),
        languages = linkedSetOf("Russian"),
        licenseName = "MIT",
        attribution = "GigaAM v2 by SberDevices, converted and packaged for sherpa-onnx.",
        approximateRamBytes = 800_000_000,
    )
}
