package com.pandulapeter.kubrikoKeyboardInputTest

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KeyboardInputTest",
    ) {
        window.minimumSize = Dimension(400, 400)
        GameKeyboardInputTest()
    }
}