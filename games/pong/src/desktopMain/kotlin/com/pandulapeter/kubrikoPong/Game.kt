package com.pandulapeter.kubrikoPong

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Pong",
    ) {
        window.minimumSize = Dimension(400, 400)
        GamePong()
    }
}