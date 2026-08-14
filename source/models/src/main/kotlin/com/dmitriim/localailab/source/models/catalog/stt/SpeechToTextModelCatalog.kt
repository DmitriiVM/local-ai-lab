package com.dmitriim.localailab.source.models.catalog.stt

import com.dmitriim.localailab.core.model.library.CatalogModel

internal object SpeechToTextModelCatalog {
    val entries: List<CatalogModel> = WhisperModelCatalog.entries + ArchiveSttModelCatalog.entries
}
