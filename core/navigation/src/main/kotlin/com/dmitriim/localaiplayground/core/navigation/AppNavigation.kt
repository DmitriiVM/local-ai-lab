package com.dmitriim.localaiplayground.core.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

enum class TopLevelDestination {
    PLAYGROUND,
    MODELS,
    RUNS,
    DEVICE,
}

enum class NavigationTarget {
    PLAYGROUND,
    MODELS,
    RUNS,
    DEVICE,
    SETTINGS,
    CHAT,
    SPEECH_TO_TEXT,
    TEXT_TO_SPEECH,
    VOICE_ASSISTANT,
}

interface AppNavigator {
    fun navigate(target: NavigationTarget)
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
