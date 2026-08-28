package com.dmitriim.localailab.source.models.validation

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.runtime.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.library.ModelValidationState
import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import com.dmitriim.localailab.core.model.manifest.ModelSource
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ModelFileValidatorTest {
    @Test
    fun validRequiredFileAndProfileProduceReadyState() = withDirectory { directory ->
        File(directory, "weights.bin").writeText("abc")
        val manifest = manifest(
            ModelFileSpec(
                relativePath = "weights.bin",
                role = ModelFileRole("WEIGHTS"),
                expectedBytes = 3,
                sha256 = SHA_ABC,
            ),
        )

        val validation = validator().validate(manifest, directory)

        assertEquals(ModelValidationState.READY, validation.first)
        assertNull(validation.second)
    }

    @Test
    fun missingRequiredFileIsReportedBeforeProfileValidation() = withDirectory { directory ->
        val validation = validator().validate(
            manifest(ModelFileSpec("weights.bin", ModelFileRole("WEIGHTS"))),
            directory,
        )

        assertEquals(ModelValidationState.MISSING_FILES, validation.first)
        assertNotNull(validation.second)
    }

    @Test
    fun unexpectedSizeIsRejected() = withDirectory { directory ->
        File(directory, "weights.bin").writeText("wrong")
        val manifest = manifest(
            ModelFileSpec(
                relativePath = "weights.bin",
                role = ModelFileRole("WEIGHTS"),
                expectedBytes = 3,
                sha256 = SHA_ABC,
            ),
        )

        val validation = validator().validate(manifest, directory)

        assertEquals(ModelValidationState.INVALID, validation.first)
        assertEquals("weights.bin has an unexpected size.", validation.second)
    }

    @Test
    fun unexpectedChecksumIsRejected() = withDirectory { directory ->
        File(directory, "weights.bin").writeText("abc")
        val manifest = manifest(
            ModelFileSpec(
                relativePath = "weights.bin",
                role = ModelFileRole("WEIGHTS"),
                expectedBytes = 3,
                sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
            ),
        )

        val validation = validator().validate(manifest, directory)

        assertEquals(ModelValidationState.INVALID, validation.first)
        assertEquals("weights.bin has an unexpected checksum.", validation.second)
    }

    @Test
    fun missingOptionalFileDoesNotInvalidateTheModel() = withDirectory { directory ->
        File(directory, "weights.bin").writeText("abc")
        val manifest = manifest(
            ModelFileSpec("weights.bin", ModelFileRole("WEIGHTS"), required = true),
            ModelFileSpec("license.txt", ModelFileRole("LICENSE"), required = false),
        )

        val validation = validator().validate(manifest, directory)

        assertEquals(ModelValidationState.READY, validation.first)
    }

    @Test
    fun unsupportedEngineAndProfileAreIncompatible() = withDirectory { directory ->
        File(directory, "weights.bin").writeText("abc")
        val manifest = manifest(ModelFileSpec("weights.bin", ModelFileRole("WEIGHTS")))

        val validation = ModelFileValidator(ModelRuntimeProfileRegistry(emptySet(), emptySet())).validate(manifest, directory)

        assertEquals(ModelValidationState.INCOMPATIBLE, validation.first)
    }

    @Test
    fun enrichChecksumsAddsMissingSizeAndDigest() = withDirectory { directory ->
        File(directory, "weights.bin").writeText("abc")
        val manifest = manifest(ModelFileSpec("weights.bin", ModelFileRole("WEIGHTS")))

        val enriched = validator().enrichChecksums(manifest, directory).files.single()

        assertEquals(3L, enriched.expectedBytes ?: -1L)
        assertEquals(SHA_ABC, enriched.sha256)
    }

    private fun validator() = ModelFileValidator(ModelRuntimeProfileRegistry(emptySet(), setOf(FakeProfile)))

    private fun manifest(vararg files: ModelFileSpec) = ModelManifest(
        modelId = ModelId("test-model"),
        displayName = "Test model",
        family = "Tests",
        capabilities = setOf(AiCapability.CHAT),
        engineId = FakeProfile.key.engineId,
        profileType = PROFILE,
        format = ModelFormat.BINARY,
        files = files.toList(),
        source = ModelSource(null, licenseName = "Test", attribution = "Test"),
        installedAtEpochMs = 0,
    )

    private inline fun withDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("model-validator").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }

    private object FakeProfile : ModelRuntimeProfile {
        override val key = ModelProfileKey(EngineId("test-engine"), PROFILE)
        override val capabilities = setOf(AiCapability.CHAT)
        override fun validate(manifest: ModelManifest, directory: File) = RuntimeValidationResult(valid = true)
    }

    private companion object {
        val PROFILE = ModelProfileId("TEST_PROFILE")
        const val SHA_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }
}
