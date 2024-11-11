package com.pandulapeter.kubrikoKeyboardInputTest

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(title = "KeyboardInputTest") {
        GameKeyboardInputTest()
    }
}