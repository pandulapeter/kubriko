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
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleClick
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseDrag
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseMove
import com.pandulapeter.gameTemplate.editor.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.FileManagerPanel
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.GameObjectManagerPanel
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.MetadataIndicatorPanel
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.selectedGameObjectHighlight
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import java.io.File

@Composable
internal fun EditorUserInterface(
    modifier: Modifier = Modifier,
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
                    onOpenIconClicked = {
                        EditorController.getExistingMapNames()
                    },
                    onSaveIconClicked = {},
                )
                EngineCanvas(
                    modifier = Modifier
                        .handleMouseMove()
                        .handleMouseZoom()
                        .handleMouseDrag()
                        .handleClick()
                        .background(Color.White),
                    editorSelectedGameObjectHighlight = { selectedGameObjectHighlight(it) },
                )
            }
            GameObjectManagerPanel(
                data = EditorController.selectedGameObject.collectAsState().value,
                selectedGameObjectType = EditorController.selectedGameObjectType.collectAsState().value,
            )
        }
        MetadataIndicatorPanel(
            gameObjectCount = EditorController.totalGameObjectCount.collectAsState().value,
            mouseWorldPosition = EditorController.mouseWorldPosition.collectAsState().value,
        )
    }
}
