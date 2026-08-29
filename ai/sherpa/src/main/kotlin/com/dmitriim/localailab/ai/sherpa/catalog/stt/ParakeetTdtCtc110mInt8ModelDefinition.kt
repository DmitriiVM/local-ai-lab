package com.dmitriim.localailab.ai.sherpa.catalog.stt

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.stt.profiles.ParakeetCtcSttProfile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class ParakeetTdtCtc110mInt8ModelDefinition(
    override val runtimeProfile: ParakeetCtcSttProfile,
) : ModelCatalogContribution {
    override val catalogModel = archiveSttCatalogModel(
        modelId = "parakeet-tdt-ctc-110m-en-int8",
        displayName = "Parakeet TDT-CTC 110M INT8",
        family = "Parakeet",
        description = "An English offline speech-to-text model quantized to INT8.",
        profileKey = runtimeProfile.key,
        archiveName = "sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8",
        archiveBytes = 104_337_827,
        archiveSha256 = "17f945007b52ccd8b7200ffc7c5652e9e8e961dfdf479cefcabd06cf5703630b",
        files = listOf(
            ModelFileSpec("model.int8.onnx", ModelFileRoles.PRIMARY_MODEL, expectedBytes = 131_652_171),
            ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 9_953),
        ),
        languages = linkedSetOf("English"),
        licenseName = "CC-BY-4.0",
        attribution = "NVIDIA Parakeet TDT-CTC 110M, converted and packaged for sherpa-onnx.",
        approximateRamBytes = 500_000_000,
    )
}
