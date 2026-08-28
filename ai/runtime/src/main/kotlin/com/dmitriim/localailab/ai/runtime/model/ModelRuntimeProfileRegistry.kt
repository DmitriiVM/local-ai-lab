package com.dmitriim.localailab.ai.runtime.model

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.manifest.ModelProfileDescriptor
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import com.dmitriim.localailab.core.model.service.ModelProfileDirectory
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * Resolves exactly one runtime profile for each persisted engine/profile key.
 *
 * Catalog-backed profiles come from [ModelCatalogContribution] so a downloadable model and its
 * runtime contract are registered together. [standaloneProfiles] is for runtime-provided models
 * that have no catalog download, such as Android system speech services.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ModelProfileDirectory>())
class ModelRuntimeProfileRegistry(
    catalogContributions: Set<ModelCatalogContribution>,
    standaloneProfiles: Set<ModelRuntimeProfile>,
) : ModelProfileDirectory {
    private val catalogProfilesByKey = catalogContributions
        .map(ModelCatalogContribution::runtimeProfile)
        .groupBy(ModelRuntimeProfile::key)
        .mapValues { (key, profiles) ->
            require(profiles.map { it::class }.distinct().size == 1) {
                "More than one runtime profile implementation backs ${key.label}."
            }
            profiles.first()
        }

    private val byKey = buildMap {
        putAll(catalogProfilesByKey)
        standaloneProfiles.forEach { profile ->
            if (profile.key !in catalogProfilesByKey) {
                require(put(profile.key, profile) == null) {
                    "More than one packaged runtime profile declares ${profile.key.label}."
                }
            }
        }
    }

    override val profiles: List<ModelProfileDescriptor> = byKey.values
        .map { profile ->
            ModelProfileDescriptor(
                key = profile.key,
                displayName = profile.displayName,
                capabilities = profile.capabilities,
                importable = profile.importDefinition != null,
            )
        }
        .sortedWith(compareBy({ it.key.engineId.value }, { it.displayName }))

    override fun find(key: ModelProfileKey): ModelProfileDescriptor? = byKey[key]?.descriptor()

    fun runtimeProfile(key: ModelProfileKey): ModelRuntimeProfile? = byKey[key]

    fun requireRuntimeProfile(key: ModelProfileKey): ModelRuntimeProfile = requireNotNull(byKey[key]) {
        "No packaged runtime profile supports ${key.label}."
    }

    inline fun <reified T : ModelRuntimeProfile> requireTyped(key: ModelProfileKey): T = requireRuntimeProfile(key) as? T
        ?: error(
            "The packaged profile ${key.engineId.value}/${key.profileId.value} " +
                "is not a ${T::class.simpleName}.",
        )

    private fun ModelRuntimeProfile.descriptor() = ModelProfileDescriptor(
        key = key,
        displayName = displayName,
        capabilities = capabilities,
        importable = importDefinition != null,
    )

    private val ModelProfileKey.label: String
        get() = "${engineId.value}/${profileId.value}"
}
