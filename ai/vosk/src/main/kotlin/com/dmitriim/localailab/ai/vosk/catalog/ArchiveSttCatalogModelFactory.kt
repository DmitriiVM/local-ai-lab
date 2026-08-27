package com.dmitriim.localailab.ai.vosk.catalog

import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.library.CatalogArchiveFormat
import com.dmitriim.localailab.core.model.library.CatalogDownload
import com.dmitriim.localailab.core.model.library.CatalogDownloadArchive
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.core.model.library.ModelCatalogState
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelSource
import com.dmitriim.localailab.core.model.manifest.SttRecognitionMode

internal fun archiveSttCatalogModel(
    modelId: String,
    displayName: String,
    family: String,
    description: String,
    profileType: com.dmitriim.localailab.core.model.manifest.ModelProfileId,
    archiveName: String,
    archiveBytes: Long,
    archiveSha256: String,
    files: List<ModelFileSpec>,
    languages: Set<String>,
    licenseName: String,
    attribution: String,
    approximateRamBytes: Long,
    engineId: EngineId = EngineId("sherpa-onnx"),
    downloadUrl: String = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/$archiveName.tar.bz2",
    archiveFormat: CatalogArchiveFormat = CatalogArchiveFormat.TAR_BZIP2,
    recognitionMode: SttRecognitionMode = SttRecognitionMode.OFFLINE,
    quantization: String? = "INT8",
) = CatalogModel(
    manifest = ModelManifest(
        modelId = ModelId(modelId),
        displayName = displayName,
        family = family,
        description = description,
        capabilities = setOf(AiCapability.SPEECH_TO_TEXT),
        engineId = engineId,
        profileType = profileType,
        format = if (engineId.value == "vosk") ModelFormat.BINARY else ModelFormat.ONNX,
        quantization = quantization,
        architecture = family,
        revision = archiveName.substringAfterLast('-'),
        files = files,
        source = ModelSource(
            url = downloadUrl,
            revision = archiveName,
            licenseName = licenseName,
            attribution = attribution,
        ),
        languages = languages,
        supportedLanguageCount = languages.size,
        sampleRateHz = 16_000,
        sttRecognitionMode = recognitionMode,
        approximateRamBytes = approximateRamBytes,
        catalogVersion = ModelCatalogDefaults.VERSION,
        installedAtEpochMs = 0,
    ),
    state = ModelCatalogState.APPROVED,
    download = CatalogDownload(
        expectedBytes = archiveBytes,
        archive = CatalogDownloadArchive(
            url = downloadUrl,
            expectedBytes = archiveBytes,
            sha256 = archiveSha256,
            rootDirectory = archiveName,
            format = archiveFormat,
        ),
    ),
)

internal fun voskDirectories() = listOf(
    ModelFileSpec("am", ModelFileRoles.PRIMARY_MODEL, directory = true),
    ModelFileSpec("conf", ModelFileRoles.CONFIG, directory = true),
    ModelFileSpec("graph", ModelFileRoles.VOCABULARY, directory = true),
)
