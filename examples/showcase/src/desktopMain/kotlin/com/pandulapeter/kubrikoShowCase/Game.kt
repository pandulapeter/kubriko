package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pandulapeter.kubriko.sceneEditor.SceneEditorWindow
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcaseKubrikoWrapper
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcaseManager
import com.pandulapeter.kubrikoShowcase.implementation.performance.isSceneEditorVisible
import java.awt.Dimension

fun main() = application {
    val shouldShowSceneEditorWindow = isSceneEditorVisible.collectAsState()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kubriko",
    ) {
        window.minimumSize = Dimension(400, 400)
        ShowcaseGame()
    }
    if (shouldShowSceneEditorWindow.value) {
        SceneEditorWindow(
            defaultMapFilename = PerformanceShowcaseManager.SCENE_NAME,
            serializationManager = PerformanceShowcaseKubrikoWrapper().serializationManager,
            onCloseRequest = { isSceneEditorVisible.value = false },
        )
    }
}