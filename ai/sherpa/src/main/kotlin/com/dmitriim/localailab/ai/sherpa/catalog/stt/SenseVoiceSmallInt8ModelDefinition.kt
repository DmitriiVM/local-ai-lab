package com.dmitriim.localailab.ai.sherpa.catalog.stt

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.stt.profiles.SenseVoiceSttProfile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class SenseVoiceSmallInt8ModelDefinition(
    override val runtimeProfile: SenseVoiceSttProfile,
) : ModelCatalogContribution {
    override val catalogModel = archiveSttCatalogModel(
        modelId = "sensevoice-small-5lang-int8",
        displayName = "SenseVoice Small INT8",
        family = "SenseVoice",
        description = "An offline speech-to-text model supporting Chinese, English, Japanese, Korean, and Cantonese.",
        profileKey = runtimeProfile.key,
        archiveName = "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17",
        archiveBytes = 163_002_883,
        archiveSha256 = "7d1efa2138a65b0b488df37f8b89e3d91a60676e416f515b952358d83dfd347e",
        files = listOf(
            ModelFileSpec("model.int8.onnx", ModelFileRoles.PRIMARY_MODEL),
            ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS),
        ),
        languages = linkedSetOf("Chinese", "English", "Japanese", "Korean", "Cantonese"),
        licenseName = "See upstream model license",
        attribution = "SenseVoiceSmall by FunAudioLLM, converted and packaged for sherpa-onnx.",
        approximateRamBytes = 700_000_000,
    )
}
