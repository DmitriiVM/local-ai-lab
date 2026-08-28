package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.stt.WeNetCtcSttProfile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

/** Catalog entry for the INT8 WeNetSpeech-Yue U2++ Conformer CTC model. */
@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class WeNetSpeechYueInt8ModelDefinition(
    override val runtimeProfile: WeNetCtcSttProfile,
) : ModelCatalogContribution {
    override val catalogModel = archiveSttCatalogModel(
        modelId = "wenetspeech-yue-u2pp-conformer-ctc-int8",
        displayName = "WeNetSpeech Yue U2++ Conformer CTC INT8",
        family = "WeNetSpeech Yue",
        description = "An offline Cantonese speech-to-text model with Chinese and English support.",
        profileKey = runtimeProfile.key,
        archiveName = ARCHIVE_NAME,
        archiveBytes = 117_203_117,
        archiveSha256 = "8636295785a43538a1b4620f167bcb89c10ce5ebdcee61c72a388738b783f992",
        files = listOf(
            ModelFileSpec("model.int8.onnx", ModelFileRoles.PRIMARY_MODEL),
            ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS),
        ),
        languages = linkedSetOf("Cantonese", "Chinese", "English"),
        licenseName = "Apache-2.0",
        attribution = "WenetSpeech-Yue by ASLP-lab, converted and packaged for sherpa-onnx.",
        approximateRamBytes = 500_000_000,
    )

    private companion object {
        const val ARCHIVE_NAME =
            "sherpa-onnx-wenetspeech-yue-u2pp-conformer-ctc-zh-en-cantonese-int8-2025-09-10"
    }
}
