package com.pandulapeter.kubriko.demoPerformance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolderImpl
import com.pandulapeter.kubriko.demoPerformance.implementation.isSceneEditorVisible
import com.pandulapeter.kubriko.demoPerformance.implementation.managers.PerformanceDemoManager
import com.pandulapeter.kubriko.demoPerformance.implementation.sceneJson
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubriko.sceneEditor.openSceneEditor

fun main() = openSceneEditor(
    defaultSceneFilename = PerformanceDemoManager.SCENE_NAME,
    serializationManager = PerformanceDemoStateHolderImpl().serializationManager,
)

@Composable
fun PerformanceDemoSceneEditor(
    defaultSceneFolderPath: String,
) {
    if (isSceneEditorVisible.collectAsState().value) {
        SceneEditor(
            defaultSceneFilename = PerformanceDemoManager.SCENE_NAME,
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = PerformanceDemoStateHolderImpl().serializationManager,
            title = "Scene Editor - Performance Demo",
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