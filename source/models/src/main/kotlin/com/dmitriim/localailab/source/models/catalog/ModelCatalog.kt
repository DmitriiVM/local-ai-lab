package com.dmitriim.localailab.source.models.catalog

import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.source.models.catalog.chat.ChatModelCatalog
import com.dmitriim.localailab.source.models.catalog.chat.LiteRtLmChatModelCatalog
import com.dmitriim.localailab.source.models.catalog.stt.SpeechToTextModelCatalog
import com.dmitriim.localailab.source.models.catalog.tts.chatterboxCatalogEntry
import com.dmitriim.localailab.source.models.catalog.tts.kokoroCatalogEntry
import com.dmitriim.localailab.source.models.catalog.tts.piperCatalogEntry
import com.dmitriim.localailab.source.models.catalog.tts.pocketTtsCatalogEntry
import com.dmitriim.localailab.source.models.catalog.tts.supertonicCatalogEntry

/** Immutable, app-bundled catalog. Remote hosts provide bytes only, never catalog updates. */
internal object ModelCatalog {
    val entries: List<CatalogModel> = buildList {
        add(chatterboxCatalogEntry)
        addAll(ChatModelCatalog.entries)
        addAll(LiteRtLmChatModelCatalog.entries)
        add(pocketTtsCatalogEntry)
        add(kokoroCatalogEntry)
        addAll(SpeechToTextModelCatalog.entries)
        add(supertonicCatalogEntry)
        add(piperCatalogEntry)
    }
}
