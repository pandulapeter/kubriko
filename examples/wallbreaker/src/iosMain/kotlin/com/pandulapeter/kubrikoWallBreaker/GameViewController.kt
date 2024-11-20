package com.pandulapeter.kubrikoWallbreaker

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

fun GameViewController() = ComposeUIViewController {
    WallbreakerGame(
        modifier = Modifier
            .systemBarsPadding()
            .displayCutoutPadding()
            .imePadding(),
    )
}