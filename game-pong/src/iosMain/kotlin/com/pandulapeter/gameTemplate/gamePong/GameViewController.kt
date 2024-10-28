package com.pandulapeter.gameTemplate.gamePong

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

fun GameViewController() = ComposeUIViewController {
    GamePong(
        modifier = Modifier.systemBarsPadding(),
    )
}