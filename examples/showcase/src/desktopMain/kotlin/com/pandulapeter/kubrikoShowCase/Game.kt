package com.pandulapeter.kubrikoShowcase

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kubriko Showcase",
    ) {
        window.minimumSize = Dimension(400, 400)
        ShowcaseGame()
    }
}