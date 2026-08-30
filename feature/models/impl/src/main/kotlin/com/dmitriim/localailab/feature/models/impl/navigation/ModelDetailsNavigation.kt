package com.dmitriim.localailab.feature.models.impl.navigation

import androidx.compose.runtime.Composable
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.feature.models.api.navigation.ModelDetailsDestination
import com.dmitriim.localailab.feature.models.impl.presentation.ModelDetailsRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class ModelDetailsNavigation : NavigationEntryProvider {
    override val destinationType = ModelDetailsDestination::class
    override val hostDestination = TopLevelDestination.MODELS

    @Composable
    override fun Content(destination: AppDestination, navigator: AppNavigator) {
        val modelDetailsDestination = destination as ModelDetailsDestination
        ModelDetailsRoute(
            modelId = modelDetailsDestination.modelId,
            onNavigateBack = navigator::navigateBack,
        )
    }
}
