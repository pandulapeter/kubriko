package com.pandulapeter.kubrikoShaderTest

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(title = "ShaderTest") {
        GameShaderTest()
    }
}