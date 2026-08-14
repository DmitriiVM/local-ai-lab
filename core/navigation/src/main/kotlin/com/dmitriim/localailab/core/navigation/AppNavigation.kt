package com.dmitriim.localailab.core.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

enum class TopLevelDestination {
    PLAYGROUND,
    MODELS,
    RUNS,
    SETTINGS,
}

enum class NavigationTarget {
    PLAYGROUND,
    MODELS,
    RUNS,
    DEVICE,
    BENCHMARK,
    SETTINGS,
    ASSISTANT,
    SPEECH_TO_TEXT,
    TEXT_TO_SPEECH,
}

interface AppNavigator {
    fun navigate(target: NavigationTarget)
    fun navigate(key: NavKey, host: TopLevelDestination)
    fun navigateBack()
}

interface NavigationEntryProvider {
    val target: NavigationTarget
    val topLevelDestination: TopLevelDestination?
        get() = null
    val hostDestination: TopLevelDestination?
        get() = topLevelDestination
    val startKey: NavKey

    fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>?
}
