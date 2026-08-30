package com.dmitriim.localailab.ai.vosk.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.api.model.library.CatalogArchiveFormat
import com.dmitriim.localailab.ai.api.model.manifest.SttRecognitionMode
import com.dmitriim.localailab.ai.vosk.VoskRuntimeProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class VoskSmallRussianModelDefinition(
    override val runtimeProfile: VoskRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = archiveSttCatalogModel(
        modelId = "vosk-small-ru-0-22",
        displayName = "Vosk Small Russian",
        family = "Vosk",
        description = "A lightweight streaming speech-to-text model for Russian.",
        profileKey = runtimeProfile.key,
        archiveName = "vosk-model-small-ru-0.22",
        downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip",
        archiveBytes = 46_236_750,
        archiveSha256 = "961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4",
        archiveFormat = CatalogArchiveFormat.ZIP,
        files = voskDirectories(),
        languages = linkedSetOf("Russian"),
        licenseName = "Apache-2.0",
        attribution = "Vosk lightweight Russian model by Alpha Cephei.",
        recognitionMode = SttRecognitionMode.STREAMING,
        quantization = null,
        approximateRamBytes = 300_000_000,
    )
}
