package com.dmitriim.localaiplayground.source.models.catalog.stt

import com.dmitriim.localaiplayground.core.model.library.CatalogModel

internal object SpeechToTextModelCatalog {
    val entries: List<CatalogModel> = WhisperModelCatalog.entries + ArchiveSttModelCatalog.entries
}
