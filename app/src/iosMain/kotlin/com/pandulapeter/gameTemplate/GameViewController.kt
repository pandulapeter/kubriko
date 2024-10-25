package com.pandulapeter.gameTemplate

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

fun GameViewController() = ComposeUIViewController {
    App(
        modifier = Modifier.systemBarsPadding(),
    )
}