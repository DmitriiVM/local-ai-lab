package com.dmitriim.localaiplayground.feature.models.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.navigation.AppNavigator
import com.dmitriim.localaiplayground.core.navigation.NavigationEntryProvider
import com.dmitriim.localaiplayground.core.navigation.NavigationTarget
import com.dmitriim.localaiplayground.core.navigation.TopLevelDestination
import com.dmitriim.localaiplayground.feature.models.presentation.ModelDetailsRoute
import com.dmitriim.localaiplayground.feature.models.presentation.ModelsRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class ModelsNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.MODELS
    override val topLevelDestination = TopLevelDestination.MODELS
    override val startKey: NavKey = ModelsKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = when (key) {
        ModelsKey -> NavEntry(key) {
            ModelsRoute(navigator)
        }
        is ModelDetailsKey -> NavEntry(key) {
            ModelDetailsRoute(
                modelId = key.modelId,
                onNavigateBack = navigator::navigateBack,
            )
        }
        else -> null
    }
}
