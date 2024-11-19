package com.pandulapeter.kubrikoPong

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

fun GameViewController() = ComposeUIViewController {
    PongGame(
        modifier = Modifier
            .systemBarsPadding()
            .displayCutoutPadding()
            .imePadding(),
    )
}