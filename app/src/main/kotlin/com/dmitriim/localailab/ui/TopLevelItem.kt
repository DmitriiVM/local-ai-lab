package com.dmitriim.localailab.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.dmitriim.localailab.core.navigation.TopLevelDestination

internal enum class TopLevelItem(
    val destination: TopLevelDestination,
    val label: String,
    val icon: ImageVector,
) {
    PLAYGROUND(TopLevelDestination.PLAYGROUND, "Playground", Icons.Outlined.Home),
    MODELS(TopLevelDestination.MODELS, "Models", Icons.Outlined.Inventory2),
    RUNS(TopLevelDestination.RUNS, "Runs", Icons.Outlined.History),
    SETTINGS(TopLevelDestination.SETTINGS, "Settings", Icons.Outlined.Settings),
}
