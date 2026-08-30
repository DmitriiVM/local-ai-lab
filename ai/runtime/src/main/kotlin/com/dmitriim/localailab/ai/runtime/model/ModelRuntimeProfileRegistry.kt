package com.dmitriim.localailab.ai.runtime.model

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Resolves exactly one runtime profile for each persisted engine/profile key.
 *
 * Catalog-backed profiles come from [ModelCatalogContribution] so a downloadable model and its
 * runtime contract are registered together. [standaloneProfiles] is for runtime-provided models
 * that have no catalog download, such as Android system speech services.
 */
@Inject
@SingleIn(AppScope::class)
class ModelRuntimeProfileRegistry(
    catalogContributions: Set<ModelCatalogContribution>,
    standaloneProfiles: Set<ModelRuntimeProfile>,
) {
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

    fun runtimeProfile(key: ModelProfileKey): ModelRuntimeProfile? = byKey[key]

    fun requireRuntimeProfile(key: ModelProfileKey): ModelRuntimeProfile = requireNotNull(byKey[key]) {
        "No packaged runtime profile supports ${key.label}."
    }

    inline fun <reified T : ModelRuntimeProfile> requireTyped(key: ModelProfileKey): T = requireRuntimeProfile(key) as? T
        ?: error(
            "The packaged profile ${key.engineId.value}/${key.profileId.value} " +
                "is not a ${T::class.simpleName}.",
        )

    private val ModelProfileKey.label: String
        get() = "${engineId.value}/${profileId.value}"
}
