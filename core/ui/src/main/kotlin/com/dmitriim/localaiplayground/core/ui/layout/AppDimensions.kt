package com.dmitriim.localaiplayground.core.ui.layout

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    val screenPadding: Dp = 20.dp,
    val sectionSpacing: Dp = 16.dp,
    val itemSpacing: Dp = 12.dp,
    val minimumTouchTarget: Dp = 48.dp,
    val navigationRailBreakpoint: Dp = 720.dp,
    val topBarOverlayClearance: Dp = 68.dp,
    val bottomNavigationOverlayClearance: Dp = 0.dp,
)

val LocalAppDimensions = staticCompositionLocalOf { AppDimensions() }
