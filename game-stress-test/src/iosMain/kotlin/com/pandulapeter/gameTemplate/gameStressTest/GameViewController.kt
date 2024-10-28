package com.pandulapeter.gameTemplate.gameStressTest

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

fun GameViewController() = ComposeUIViewController {
    GameStressTest(
        modifier = Modifier.systemBarsPadding(),
    )
}