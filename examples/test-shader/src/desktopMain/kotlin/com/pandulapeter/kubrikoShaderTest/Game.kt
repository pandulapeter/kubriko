package com.pandulapeter.kubrikoShaderTest

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ShaderTest",
    ) {
        window.minimumSize = Dimension(400, 400)
        GameShaderTest()
    }
}