package com.pandulapeter.kubrikoWallbreaker

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(title = "Wallbreaker") {
        WallbreakerGame()
    }
}