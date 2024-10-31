package com.pandulapeter.kubrikoStressTest

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

fun GameViewController() = ComposeUIViewController {
    GameStressTest(
        modifier = Modifier.systemBarsPadding(),
    )
}