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
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.FileManagerPanel
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.GameObjectManagerPanel
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.MetadataIndicatorPanel
import com.pandulapeter.gameTemplate.engine.EngineCanvas

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
        Row(
            modifier = Modifier.weight(1f),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                FileManagerPanel(
                    onNewIconClicked = EditorController::reset,
                    onOpenIconClicked = openFilePickerForLoading,
                    onSaveIconClicked = openFilePickerForSaving,
                )
                EngineCanvas(
                    modifier = Modifier
                        .handleMouseClick()
                        .handleMouseMove()
                        .handleMouseZoom()
                        .handleMouseDrag()
                        .background(Color.White),
                )
            }
            GameObjectManagerPanel(
                data = EditorController.selectedGameObject.collectAsState().value,
                selectedGameObjectTypeId = EditorController.selectedGameObjectTypeId.collectAsState().value,
            )
        }
        MetadataIndicatorPanel(
            gameObjectCount = EditorController.totalGameObjectCount.collectAsState().value,
            mouseWorldCoordinates = EditorController.mouseWorldCoordinates.collectAsState().value,
        )
    }
}