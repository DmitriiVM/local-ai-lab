package com.dmitriim.localailab.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.core.ui.style.AppSurfaceStyle

@Composable
fun AdaptiveNavigationScaffold(
    selectedDestination: TopLevelDestination,
    showTopLevelNavigation: Boolean,
    onSelectDestination: (AppDestination) -> Unit,
    onNavigateUp: (() -> Unit)?,
    toolbarTitle: String?,
    content: @Composable (Modifier) -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(AppSurfaceStyle.pageBackgroundBrush(MaterialTheme.colorScheme)),
    ) {
        if (maxWidth >= dimensions.navigationRailBreakpoint) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (showTopLevelNavigation) {
                    NavigationRail {
                        TopLevelItem.entries.forEach { item ->
                            NavigationRailItem(
                                selected = selectedDestination == item.hostDestination,
                                onClick = { onSelectDestination(item.destination) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        contentWindowInsets = WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ) { contentPadding ->
                        content(Modifier.padding(contentPadding))
                    }
                    AppTopBar(
                        onNavigateUp = onNavigateUp,
                        title = toolbarTitle,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalAppDimensions provides dimensions.copy(
                        bottomNavigationOverlayClearance = if (showTopLevelNavigation) 100.dp else 0.dp,
                    ),
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        contentWindowInsets = WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ) { contentPadding ->
                        content(Modifier.padding(contentPadding))
                    }
                }
                if (showTopLevelNavigation) {
                    LiquidGlassNavigationBar(
                        selectedDestination = selectedDestination,
                        onSelectDestination = onSelectDestination,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                AppTopBar(
                    onNavigateUp = onNavigateUp,
                    title = toolbarTitle,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        }
    }
}
