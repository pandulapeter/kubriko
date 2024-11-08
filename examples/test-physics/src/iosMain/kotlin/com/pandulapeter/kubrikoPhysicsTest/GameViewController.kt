package com.pandulapeter.kubrikoPhysicsTest

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

fun GameViewController() = ComposeUIViewController {
    GamePhysicsTest(
        modifier = Modifier
            .systemBarsPadding()
            .displayCutoutPadding()
            .imePadding(),
    )
}