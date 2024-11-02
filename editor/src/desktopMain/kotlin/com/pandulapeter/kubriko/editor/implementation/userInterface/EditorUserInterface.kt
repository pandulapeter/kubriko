package com.pandulapeter.kubriko.editor.implementation.userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.editor.implementation.EditorController
import com.pandulapeter.kubriko.editor.implementation.extensions.handleMouseClick
import com.pandulapeter.kubriko.editor.implementation.extensions.handleMouseDrag
import com.pandulapeter.kubriko.editor.implementation.extensions.handleMouseMove
import com.pandulapeter.kubriko.editor.implementation.extensions.handleMouseZoom
import com.pandulapeter.kubriko.editor.implementation.userInterface.panels.fileManagerRow.FileManagerRow
import com.pandulapeter.kubriko.editor.implementation.userInterface.panels.instanceBrowserColumn.InstanceBrowserColumn
import com.pandulapeter.kubriko.editor.implementation.userInterface.panels.instanceManagerColumn.InstanceManagerColumn
import com.pandulapeter.kubriko.editor.implementation.userInterface.panels.metadataRow.MetadataRow
import com.pandulapeter.kubriko.engine.EngineCanvas

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
                resolveTypeId = editorController.kubriko.serializationManager::resolveTypeId,
            )
            EngineCanvas(
                modifier = Modifier
                    .weight(1f)
                    .handleMouseClick(
                        getSelectedInstance = editorController::getSelectedInstance,
                        getMouseSceneOffset = editorController::getMouseWorldCoordinates,
                        onLeftClick = editorController::onLeftClick,
                        onRightClick = editorController::onRightClick,
                    )
                    .handleMouseMove(
                        onMouseMove = editorController::onMouseMove,
                    )
                    .handleMouseZoom(
                        viewportManager = editorController.kubriko.viewportManager,
                    )
                    .handleMouseDrag(
                        inputManager = editorController.kubriko.inputManager,
                        viewportManager = editorController.kubriko.viewportManager,
                        getSelectedInstance = editorController::getSelectedInstance,
                        getMouseSceneOffset = editorController::getMouseWorldCoordinates,
                        notifySelectedInstanceUpdate = editorController::notifySelectedInstanceUpdate,
                    )
                    .background(Color.White),
                kubriko = editorController.kubriko,
            )
            InstanceManagerColumn(
                registeredTypeIds = editorController.kubriko.serializationManager.typeIdsForEditor.toList(),
                selectedTypeId = editorController.selectedTypeId.collectAsState().value,
                selectedUpdatableInstance = editorController.selectedUpdatableInstance.collectAsState().value,
                selectTypeId = editorController::selectInstance,
                resolveTypeId = editorController.kubriko.serializationManager::resolveTypeId,
                deselectSelectedInstance = editorController::deselectSelectedInstance,
                locateSelectedInstance = editorController::locateSelectedInstance,
                deleteSelectedInstance = editorController::deleteSelectedInstance,
                notifySelectedInstanceUpdate = editorController::notifySelectedInstanceUpdate,
            )
        }
        MetadataRow(
            totalActorCount = editorController.totalActorCount.collectAsState().value,
            mouseSceneOffset = editorController.mouseSceneOffset.collectAsState().value,
        )
    }
}