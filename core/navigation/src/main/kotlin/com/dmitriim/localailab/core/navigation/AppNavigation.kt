package com.dmitriim.localailab.core.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

enum class TopLevelDestination {
    PLAYGROUND,
    MODELS,
    RUNS,
    SETTINGS,
}

interface AppNavigator {
    fun navigate(destination: AppDestination)
    fun navigateBack()
}

interface NavigationEntryProvider {
    val destinationType: KClass<out AppDestination>
    val hostDestination: TopLevelDestination
    val isRootDestination: Boolean
        get() = false

    @Composable
    fun Content(destination: AppDestination, navigator: AppNavigator)
}
