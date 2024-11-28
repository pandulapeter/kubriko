package com.pandulapeter.kubriko.sceneEditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.application
import com.pandulapeter.kubriko.sceneEditor.implementation.InternalSceneEditor
import com.pandulapeter.kubriko.serialization.SerializationManager

fun openSceneEditor(
    defaultSceneFilename: String? = null,
    defaultSceneFolderPath: String = "./src/commonMain/composeResources/files/scenes",
    serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
) = application {
    SceneEditor(
        defaultSceneFilename = defaultSceneFilename,
        defaultSceneFolderPath = defaultSceneFolderPath,
        serializationManager = serializationManager,
        onCloseRequest = ::exitApplication,
    )
}

sealed class SceneEditorMode {

    data object Normal : SceneEditorMode()

    data class Connected(
        val sceneJson: String,
        val onSceneJsonChanged: (String) -> Unit,
    ) : SceneEditorMode()
}

@Composable
fun SceneEditor(
    defaultSceneFilename: String? = null,
    defaultSceneFolderPath: String = "./src/commonMain/composeResources/files/scenes",
    serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
    sceneEditorMode: SceneEditorMode = SceneEditorMode.Normal,
    onCloseRequest: () -> Unit,
) = InternalSceneEditor(
    defaultSceneFilename = defaultSceneFilename,
    defaultSceneFolderPath = defaultSceneFolderPath,
    serializationManager = serializationManager,
    sceneEditorMode = sceneEditorMode,
    onCloseRequest = onCloseRequest,
)