package com.pandulapeter.kubrikoPhysicsTest

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(title = "PhysicsTest") {
        GamePhysicsTest()
    }
}