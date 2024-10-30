package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseClick
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseDrag
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseMove
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.FileManagerRow
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.InstanceManagerColumn
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.InstanceBrowserColumn
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.MetadataRow
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor

@Composable
internal fun EditorUserInterface(
    modifier: Modifier = Modifier,
    openFilePickerForLoading: () -> Unit,
    openFilePickerForSaving: () -> Unit,
) = MaterialTheme(
    colors = lightColors(
        primary = Color.DarkGray,
        secondary = Color.DarkGray,
    ),
) {
    Column(
        modifier = modifier,
    ) {
        FileManagerRow(
            onNewIconClicked = EditorController::reset,
            onOpenIconClicked = openFilePickerForLoading,
            onSaveIconClicked = openFilePickerForSaving,
        )
        Row(
            modifier = Modifier.weight(1f),
        ) {
            InstanceBrowserColumn(
                allGameObjects = Engine.get().instanceManager.gameObjects.collectAsState().value.filterIsInstance<AvailableInEditor<*>>(),
                visibleGameObjects = Engine.get().instanceManager.visibleGameObjectsWithinViewport.collectAsState().value.filterIsInstance<AvailableInEditor<*>>(),
            )
            EngineCanvas(
                modifier = Modifier
                    .weight(1f)
                    .handleMouseClick()
                    .handleMouseMove()
                    .handleMouseZoom()
                    .handleMouseDrag()
                    .background(Color.White),
            )
            InstanceManagerColumn(
                data = EditorController.selectedGameObject.collectAsState().value,
                selectedGameObjectTypeId = EditorController.selectedGameObjectTypeId.collectAsState().value,
            )
        }
        MetadataRow(
            gameObjectCount = EditorController.totalGameObjectCount.collectAsState().value,
            mouseWorldCoordinates = EditorController.mouseWorldCoordinates.collectAsState().value,
        )
    }
}