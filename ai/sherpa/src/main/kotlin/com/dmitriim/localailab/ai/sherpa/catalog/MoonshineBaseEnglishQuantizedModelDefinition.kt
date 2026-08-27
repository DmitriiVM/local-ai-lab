package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.library.ModelCatalogContribution
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class MoonshineBaseEnglishQuantizedModelDefinition : ModelCatalogContribution {
    override val catalogModel = archiveSttCatalogModel(
        modelId = "moonshine-base-en-quantized",
        displayName = "Moonshine v2 Base Quantized",
        family = "Moonshine",
        description = "A quantized English offline speech-to-text model.",
        profileType = ModelProfileIds.MOONSHINE_STT,
        archiveName = "sherpa-onnx-moonshine-base-en-quantized-2026-02-27",
        archiveBytes = 111_266_225,
        archiveSha256 = "43232c1d13013d37317163baec3135bd771a186a4356f28c889bab453bb0e891",
        files = listOf(
            ModelFileSpec("encoder_model.ort", ModelFileRoles.ENCODER, expectedBytes = 31_326_816),
            ModelFileSpec("decoder_model_merged.ort", ModelFileRoles.MERGED_DECODER, expectedBytes = 109_424_400),
            ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 549_350),
        ),
        languages = linkedSetOf("English"),
        licenseName = "CC-BY-4.0",
        attribution = "Moonshine v2 by Useful Sensors, quantized and packaged for sherpa-onnx.",
        approximateRamBytes = 550_000_000,
    )
}
