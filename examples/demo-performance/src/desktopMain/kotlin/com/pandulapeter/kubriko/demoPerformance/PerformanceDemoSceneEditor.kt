package com.pandulapeter.kubriko.demoPerformance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoKubrikoWrapper
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoManager
import com.pandulapeter.kubriko.demoPerformance.implementation.isSceneEditorVisible
import com.pandulapeter.kubriko.demoPerformance.implementation.sceneJson
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode

@Composable
fun PerformanceDemoSceneEditor() {
    if (isSceneEditorVisible.collectAsState().value) {
        SceneEditor(
            defaultMapFilename = PerformanceDemoManager.SCENE_NAME,
            serializationManager = PerformanceDemoKubrikoWrapper().serializationManager,
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