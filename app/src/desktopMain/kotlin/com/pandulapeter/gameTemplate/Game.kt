package com.pandulapeter.gameTemplate

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Game",
    ) {
        window.minimumSize = Dimension(400, 400)
        App(
            exit = ::exitApplication,
        )
    }
}