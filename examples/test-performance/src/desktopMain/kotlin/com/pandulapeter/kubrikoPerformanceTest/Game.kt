package com.pandulapeter.kubrikoPerformanceTest

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PerformanceTest",
    ) {
        window.minimumSize = Dimension(400, 400)
        GamePerformanceTest()
    }
}