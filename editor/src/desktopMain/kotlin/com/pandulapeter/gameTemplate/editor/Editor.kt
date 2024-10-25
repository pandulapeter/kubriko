package com.pandulapeter.gameTemplate.editor

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Editor",
    ) {
        window.minimumSize = Dimension(400, 400)
        EditorApp()
    }
}