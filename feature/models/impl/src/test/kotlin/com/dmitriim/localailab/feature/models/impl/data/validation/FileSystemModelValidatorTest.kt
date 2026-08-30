package com.dmitriim.localailab.feature.models.impl.data.validation

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRole
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.ai.api.model.manifest.ModelFormat
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import com.dmitriim.localailab.ai.api.model.manifest.ModelSource
import com.dmitriim.localailab.ai.runtime.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FileSystemModelValidatorTest {
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

        assertEquals(ModelValidationState.READY, validation.state)
        assertNull(validation.message)
    }

    @Test
    fun missingRequiredFileIsReportedBeforeProfileValidation() = withDirectory { directory ->
        val validation = validator().validate(
            manifest(ModelFileSpec("weights.bin", ModelFileRole("WEIGHTS"))),
            directory,
        )

        assertEquals(ModelValidationState.MISSING_FILES, validation.state)
        assertNotNull(validation.message)
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

        assertEquals(ModelValidationState.INVALID, validation.state)
        assertEquals("weights.bin has an unexpected size.", validation.message)
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

        assertEquals(ModelValidationState.INVALID, validation.state)
        assertEquals("weights.bin has an unexpected checksum.", validation.message)
    }

    @Test
    fun missingOptionalFileDoesNotInvalidateTheModel() = withDirectory { directory ->
        File(directory, "weights.bin").writeText("abc")
        val manifest = manifest(
            ModelFileSpec("weights.bin", ModelFileRole("WEIGHTS"), required = true),
            ModelFileSpec("license.txt", ModelFileRole("LICENSE"), required = false),
        )

        val validation = validator().validate(manifest, directory)

        assertEquals(ModelValidationState.READY, validation.state)
    }

    @Test
    fun unsupportedEngineAndProfileAreIncompatible() = withDirectory { directory ->
        File(directory, "weights.bin").writeText("abc")
        val manifest = manifest(ModelFileSpec("weights.bin", ModelFileRole("WEIGHTS")))

        val validation = FileSystemModelValidator(ModelRuntimeProfileRegistry(emptySet(), emptySet())).validate(manifest, directory)

        assertEquals(ModelValidationState.INCOMPATIBLE, validation.state)
    }

    @Test
    fun enrichChecksumsAddsMissingSizeAndDigest() = withDirectory { directory ->
        File(directory, "weights.bin").writeText("abc")
        val manifest = manifest(ModelFileSpec("weights.bin", ModelFileRole("WEIGHTS")))

        val enriched = validator().enrichChecksums(manifest, directory).files.single()

        assertEquals(3L, enriched.expectedBytes ?: -1L)
        assertEquals(SHA_ABC, enriched.sha256)
    }

    private fun validator() = FileSystemModelValidator(ModelRuntimeProfileRegistry(emptySet(), setOf(FakeProfile)))

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
        override fun validate(manifest: ModelManifest, directory: File) = RuntimeValidationResult(valid = true)
    }

    private companion object {
        val PROFILE = ModelProfileId("TEST_PROFILE")
        const val SHA_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }
}
