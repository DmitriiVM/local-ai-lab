package com.dmitriim.localailab.ai.vosk.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.library.CatalogArchiveFormat
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.vosk.VoskRuntimeProfile
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.SttRecognitionMode
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class VoskSmallEnglishUsModelDefinition(
    override val runtimeProfile: VoskRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = archiveSttCatalogModel(
        modelId = "vosk-small-en-us-0-15",
        displayName = "Vosk Small English US",
        family = "Vosk",
        description = "A lightweight streaming speech-to-text model for US English.",
        engineId = EngineId("vosk"),
        profileType = ModelProfileIds.VOSK_STT,
        archiveName = "vosk-model-small-en-us-0.15",
        downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
        archiveBytes = 41_205_931,
        archiveSha256 = "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498",
        archiveFormat = CatalogArchiveFormat.ZIP,
        files = voskDirectories(),
        languages = linkedSetOf("English"),
        licenseName = "Apache-2.0",
        attribution = "Vosk lightweight US English model by Alpha Cephei.",
        recognitionMode = SttRecognitionMode.STREAMING,
        quantization = null,
        approximateRamBytes = 300_000_000,
    )
}
