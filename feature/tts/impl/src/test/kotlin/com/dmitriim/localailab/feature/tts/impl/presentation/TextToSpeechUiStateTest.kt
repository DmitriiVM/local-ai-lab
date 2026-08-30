package com.dmitriim.localailab.feature.tts.impl.presentation

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.library.CatalogDownload
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogState
import com.dmitriim.localailab.ai.api.model.manifest.ModelFormat
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.manifest.ModelSource
import com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceDescriptor
import com.dmitriim.localailab.ai.api.system.SystemTextToSpeechVoice
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextToSpeechUiStateTest {
    @Test
    fun `compatible voices normalize locale tags and exclude incompatible languages`() {
        val model = TtsModelOption(
            id = ModelId("model"),
            displayName = "Model",
            engineId = EngineId("engine"),
            profileType = ModelProfileId("PIPER_VITS_TTS"),
            languages = setOf("en-US", "ru"),
            speakerCount = 2,
            voiceMode = com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceMode.SPEAKER_ID,
            supportedControls = emptySet(),
            voices = listOf(
                voice("english", setOf("en_GB")),
                voice("russian", setOf("ru")),
                voice("unrestricted", emptySet()),
            ),
            installed = true,
        )

        assertEquals(listOf("english", "unrestricted"), model.compatibleVoices(TtsLanguage.ENGLISH).map { it.id })
        assertEquals(listOf("russian", "unrestricted"), model.compatibleVoices(TtsLanguage.RUSSIAN).map { it.id })
        assertTrue(model.compatibleVoices(TtsLanguage.CHINESE).isEmpty())
    }

    @Test
    fun `installed model keeps current metadata from the matching catalog revision`() {
        val installedManifest = manifest(displayName = "Installed", voices = emptyList())
        val catalogManifest = manifest(
            displayName = "Catalog",
            voices = listOf(TtsVoiceDescriptor("catalog-voice", "Catalog voice", 0, setOf("en"))),
        )
        val installed = InstalledModel(
            manifest = installedManifest,
            localDirectoryName = "model",
            totalBytes = 1,
            validationState = ModelValidationState.READY,
        )

        val option = ttsModelOptions(
            installedModels = listOf(installed),
            catalogModels = listOf(
                CatalogModel(
                    catalogManifest,
                    ModelCatalogState.APPROVED,
                    CatalogDownload(expectedBytes = 1),
                ),
            ),
        ).single()

        assertTrue(option.installed)
        assertEquals("Installed", option.displayName)
        assertEquals(listOf("catalog-voice"), option.voices.map { it.id })
    }

    @Test
    fun `platform voices retain their stable platform IDs`() {
        val option = androidTextToSpeechOption(
            listOf(SystemTextToSpeechVoice("platform-en", "English", "en-US", "Device voice")),
        )

        assertEquals("platform-en", option.voices.single().platformVoiceId)
        assertEquals(setOf("English"), option.languages)
    }

    private fun voice(id: String, languages: Set<String>) = TtsVoiceOption(
        id = id,
        displayName = id,
        speakerId = 0,
        languages = languages,
        description = null,
    )

    private fun manifest(displayName: String, voices: List<TtsVoiceDescriptor>) = ModelManifest(
        modelId = ModelId("model"),
        displayName = displayName,
        family = "Test",
        capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
        engineId = EngineId("engine"),
        profileType = ModelProfileId("PIPER_VITS_TTS"),
        format = ModelFormat.ONNX,
        revision = "r1",
        files = emptyList(),
        source = ModelSource(null, licenseName = "Test", attribution = "Test"),
        languages = setOf("en"),
        speakerCount = 1,
        voices = voices,
        installedAtEpochMs = 0,
    )
}
