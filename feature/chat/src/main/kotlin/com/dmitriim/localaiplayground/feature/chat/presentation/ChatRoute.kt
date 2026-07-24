package com.dmitriim.localaiplayground.feature.chat.presentation

import androidx.compose.runtime.Composable
import com.dmitriim.localaiplayground.feature.chat.presentation.ui.ChatScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ChatRoute(viewModel: ChatViewModel = metroViewModel()) {
    ChatScreen()
}
