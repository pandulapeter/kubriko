package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcaseKubrikoWrapper
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcaseManager
import com.pandulapeter.kubrikoShowcase.implementation.performance.isSceneEditorVisible
import com.pandulapeter.kubrikoShowcase.implementation.performance.sceneJson
import java.awt.Dimension

fun main() = application {
    val shouldShowSceneEditorWindow = isSceneEditorVisible.collectAsState()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kubriko",
    ) {
        window.minimumSize = Dimension(400, 400)
        KubrikoShowcase()
    }
    if (shouldShowSceneEditorWindow.value) {
        SceneEditor(
            defaultMapFilename = PerformanceShowcaseManager.SCENE_NAME,
            serializationManager = PerformanceShowcaseKubrikoWrapper().serializationManager,
            onCloseRequest = { isSceneEditorVisible.value = false },
            sceneEditorMode = sceneJson?.let { sceneJson ->
                SceneEditorMode.Connected(
                    sceneJson = sceneJson.value,
                    onSceneJsonChanged = { sceneJson.value = it },
                )
            } ?: SceneEditorMode.Normal,
        )
    }
}