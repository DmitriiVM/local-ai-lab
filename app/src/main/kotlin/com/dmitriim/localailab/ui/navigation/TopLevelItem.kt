package com.dmitriim.localailab.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.core.navigation.destination.ModelsDestination
import com.dmitriim.localailab.core.navigation.destination.PlaygroundDestination
import com.dmitriim.localailab.core.navigation.destination.RunsDestination
import com.dmitriim.localailab.core.navigation.destination.SettingsDestination

internal enum class TopLevelItem(
    val hostDestination: TopLevelDestination,
    val label: String,
    val icon: ImageVector,
) {
    PLAYGROUND(TopLevelDestination.PLAYGROUND, "Playground", Icons.Outlined.Home),
    MODELS(TopLevelDestination.MODELS, "Models", Icons.Outlined.Inventory2),
    RUNS(TopLevelDestination.RUNS, "Runs", Icons.Outlined.History),
    SETTINGS(TopLevelDestination.SETTINGS, "Settings", Icons.Outlined.Settings);

    val destination: AppDestination
        get() = when (this) {
            PLAYGROUND -> PlaygroundDestination
            MODELS -> ModelsDestination
            RUNS -> RunsDestination
            SETTINGS -> SettingsDestination
        }
}
