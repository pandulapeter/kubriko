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
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.InstanceBrowserColumn
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.InstanceManagerColumn
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.MetadataRow
import com.pandulapeter.gameTemplate.engine.EngineCanvas

@Composable
internal fun EditorUserInterface(
    modifier: Modifier = Modifier,
    editorController: EditorController,
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
            currentFileName = editorController.currentFileName.collectAsState().value,
            onNewIconClicked = editorController::reset,
            onOpenIconClicked = openFilePickerForLoading,
            onSaveIconClicked = openFilePickerForSaving,
        )
        Row(
            modifier = Modifier.weight(1f),
        ) {
            InstanceBrowserColumn(
                shouldShowVisibleOnly = editorController.shouldShowVisibleOnly.collectAsState().value,
                allInstances = editorController.allInstances.collectAsState().value,
                visibleInstances = editorController.visibleInstancesWithinViewport.collectAsState().value,
                selectedUpdatableInstance = editorController.selectedUpdatableInstance.collectAsState().value,
                onShouldShowVisibleOnlyToggled = editorController::onShouldShowVisibleOnlyToggled,
                selectInstance = editorController::selectInstance,
                resolveTypeId = editorController.engine.instanceManager::resolveTypeId,
            )
            EngineCanvas(
                modifier = Modifier
                    .weight(1f)
                    .handleMouseClick(
                        getSelectedInstance = editorController::getSelectedInstance,
                        getMouseWorldCoordinates = editorController::getMouseWorldCoordinates,
                        onLeftClick = editorController::onLeftClick,
                        onRightClick = editorController::onRightClick,
                    )
                    .handleMouseMove(
                        onMouseMove = editorController::onMouseMove,
                    )
                    .handleMouseZoom(
                        viewportManager = editorController.engine.viewportManager,
                    )
                    .handleMouseDrag(
                        inputManager = editorController.engine.inputManager,
                        viewportManager = editorController.engine.viewportManager,
                        getSelectedInstance = editorController::getSelectedInstance,
                        getMouseWorldCoordinates = editorController::getMouseWorldCoordinates,
                        notifySelectedInstanceUpdate = editorController::notifySelectedInstanceUpdate,
                    )
                    .background(Color.White),
                engine = editorController.engine,
            )
            InstanceManagerColumn(
                registeredTypeIds = editorController.engine.instanceManager.typeIdsForEditor.toList(),
                selectedTypeId = editorController.selectedTypeId.collectAsState().value,
                selectedUpdatableInstance = editorController.selectedUpdatableInstance.collectAsState().value,
                selectTypeId = editorController::selectInstance,
                resolveTypeId = editorController.engine.instanceManager::resolveTypeId,
                deselectSelectedInstance = editorController::deselectSelectedInstance,
                locateSelectedInstance = editorController::locateSelectedInstance,
                deleteSelectedInstance = editorController::deleteSelectedInstance,
                notifySelectedInstanceUpdate = editorController::notifySelectedInstanceUpdate,
            )
        }
        MetadataRow(
            gameObjectCount = editorController.totalGameObjectCount.collectAsState().value,
            mouseWorldCoordinates = editorController.mouseWorldCoordinates.collectAsState().value,
        )
    }
}