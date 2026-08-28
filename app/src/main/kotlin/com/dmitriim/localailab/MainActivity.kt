package com.dmitriim.localailab

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.ui.theme.LocalAiLabTheme
import com.dmitriim.localailab.source.settings.AppSettings
import com.dmitriim.localailab.ui.LocalAiLabApp
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false

        val graph = (application as LocalAiLabApplication).graph
        setContent {
            val settings by graph.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.dark(Color.BLACK),
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
                LocalAiLabTheme {
                    LocalAiLabApp(graph)
                }
            }
        }
    }
}
