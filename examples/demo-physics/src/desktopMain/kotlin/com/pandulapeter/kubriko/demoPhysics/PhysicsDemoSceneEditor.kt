package com.pandulapeter.kubriko.demoPhysics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoManager
import com.pandulapeter.kubriko.demoPhysics.implementation.isSceneEditorVisible
import com.pandulapeter.kubriko.demoPhysics.implementation.sceneJson
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubriko.sceneEditor.openSceneEditor

fun main() = openSceneEditor(
    defaultSceneFilename = PhysicsDemoManager.SCENE_NAME,
    serializationManager = PhysicsDemoStateHolderImpl().serializationManager,
)

@Composable
fun PhysicsDemoSceneEditor(
    defaultSceneFolderPath: String,
) {
    if (isSceneEditorVisible.collectAsState().value) {
        SceneEditor(
            defaultSceneFilename = PhysicsDemoManager.SCENE_NAME,
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = PhysicsDemoStateHolderImpl().serializationManager,
            title = "Scene Editor - Physics Demo",
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