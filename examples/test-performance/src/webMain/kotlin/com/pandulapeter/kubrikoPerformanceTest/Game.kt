package com.pandulapeter.kubrikoPerformanceTest

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(title = "PerformanceTest") {
        GamePerformanceTest()
    }
}