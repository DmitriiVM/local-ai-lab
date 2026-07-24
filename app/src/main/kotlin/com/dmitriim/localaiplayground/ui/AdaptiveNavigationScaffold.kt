package com.dmitriim.localaiplayground.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dmitriim.localaiplayground.core.navigation.TopLevelDestination
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions

@Composable
fun AdaptiveNavigationScaffold(
    selectedDestination: TopLevelDestination,
    onSelectDestination: (TopLevelDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateUp: (() -> Unit)?,
    content: @Composable (Modifier) -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= dimensions.navigationRailBreakpoint) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    TopLevelItem.entries.forEach { item ->
                        NavigationRailItem(
                            selected = selectedDestination == item.destination,
                            onClick = { onSelectDestination(item.destination) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = { AppTopBar(onOpenSettings, onNavigateUp) },
                ) { contentPadding ->
                    content(Modifier.padding(contentPadding))
                }
            }
        } else {
            Scaffold(
                topBar = { AppTopBar(onOpenSettings, onNavigateUp) },
                bottomBar = {
                    NavigationBar {
                        TopLevelItem.entries.forEach { item ->
                            NavigationBarItem(
                                selected = selectedDestination == item.destination,
                                onClick = { onSelectDestination(item.destination) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                },
            ) { contentPadding ->
                content(Modifier.padding(contentPadding))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppTopBar(
    onOpenSettings: () -> Unit,
    onNavigateUp: (() -> Unit)?,
) {
    TopAppBar(
        title = { Text("Local AI Playground") },
        navigationIcon = {
            onNavigateUp?.let { navigateUp ->
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Navigate back",
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Open settings and privacy",
                )
            }
        },
    )
}
