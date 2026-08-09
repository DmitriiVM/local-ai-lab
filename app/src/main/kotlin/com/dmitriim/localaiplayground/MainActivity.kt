package com.dmitriim.localaiplayground

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localaiplayground.core.ui.theme.LocalAiPlaygroundTheme
import com.dmitriim.localaiplayground.source.settings.AppSettings
import com.dmitriim.localaiplayground.source.settings.ThemePreference
import com.dmitriim.localaiplayground.ui.LocalAiPlaygroundApp
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val graph = (application as LocalAiPlaygroundApplication).graph
        setContent {
            val settings by graph.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            val darkTheme = when (settings.theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            SideEffect {
                val statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                }
                val navigationBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.BLACK)
                } else {
                    SystemBarStyle.light(Color.WHITE, Color.BLACK)
                }
                enableEdgeToEdge(
                    statusBarStyle = statusBarStyle,
                    navigationBarStyle = navigationBarStyle,
                )
                if (settings.keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                LocalAiPlaygroundTheme(darkTheme = darkTheme) {
                    LocalAiPlaygroundApp(graph)
                }
            }
        }
    }
}
