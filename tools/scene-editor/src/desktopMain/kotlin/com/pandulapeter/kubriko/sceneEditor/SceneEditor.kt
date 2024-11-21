package com.pandulapeter.kubriko.sceneEditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.application
import com.pandulapeter.kubriko.sceneEditor.implementation.InternalSceneEditor
import com.pandulapeter.kubriko.serialization.SerializationManager

fun openSceneEditor(
    defaultMapFilename: String? = null,
    serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
) = application {
    SceneEditor(
        defaultMapFilename = defaultMapFilename,
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
    defaultMapFilename: String? = null,
    serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
    sceneEditorMode: SceneEditorMode = SceneEditorMode.Normal,
    onCloseRequest: () -> Unit,
) = InternalSceneEditor(
    defaultMapFilename = defaultMapFilename,
    serializationManager = serializationManager,
    sceneEditorMode = sceneEditorMode,
    onCloseRequest = onCloseRequest,
)