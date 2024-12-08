package com.pandulapeter.kubrikoShowcase

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemoSceneEditor
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemoSceneEditor
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kubriko",
    ) {
        window.minimumSize = Dimension(400, 400)
        KubrikoShowcase()
    }
    PerformanceDemoSceneEditor(
        defaultSceneFolderPath = "../examples/demo-performance/src/commonMain/composeResources/files/scenes"
    )
    PhysicsDemoSceneEditor(
        defaultSceneFolderPath = "../examples/demo-physics/src/commonMain/composeResources/files/scenes"
    )
}