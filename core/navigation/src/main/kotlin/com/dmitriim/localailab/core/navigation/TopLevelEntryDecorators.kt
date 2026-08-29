package com.dmitriim.localailab.core.navigation

import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey

internal data class TopLevelEntryDecorators(
    val playground: List<NavEntryDecorator<NavKey>>,
    val models: List<NavEntryDecorator<NavKey>>,
    val runs: List<NavEntryDecorator<NavKey>>,
    val settings: List<NavEntryDecorator<NavKey>>,
) {
    operator fun get(destination: TopLevelDestination): List<NavEntryDecorator<NavKey>> = when (destination) {
        TopLevelDestination.PLAYGROUND -> playground
        TopLevelDestination.MODELS -> models
        TopLevelDestination.RUNS -> runs
        TopLevelDestination.SETTINGS -> settings
    }
}
