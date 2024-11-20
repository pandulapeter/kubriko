package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.sceneEditor.implementation.EditorController
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.handleMouseClick
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.handleMouseDrag
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.handleMouseMove
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.handleMouseZoom
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.fileManagerRow.FileManagerRow
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceBrowserColumn.InstanceBrowserColumn
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.InstanceManagerColumn
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.metadataRow.MetadataRow

@Composable
internal fun EditorUserInterface(
    modifier: Modifier = Modifier,
    editorController: EditorController,
    openFilePickerForLoading: () -> Unit,
    openFilePickerForSaving: () -> Unit,
) = KubrikoTheme {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
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
                    allInstances = editorController.allEditableActors.collectAsState().value,
                    visibleInstances = editorController.visibleActorsWithinViewport.collectAsState().value,
                    selectedUpdatableInstance = editorController.selectedUpdatableActor.collectAsState().value,
                    onShouldShowVisibleOnlyToggled = editorController::onShouldShowVisibleOnlyToggled,
                    selectInstance = editorController::selectActor,
                    resolveTypeId = editorController.serializationManager::getTypeId,
                )
                KubrikoCanvas(
                    modifier = Modifier
                        .weight(1f)
                        .handleMouseClick(
                            getSelectedActor = editorController::getSelectedActor,
                            getMouseSceneOffset = editorController::getMouseWorldCoordinates,
                            onLeftClick = editorController::onLeftClick,
                            onRightClick = editorController::onRightClick,
                        )
                        .handleMouseMove(
                            onMouseMove = editorController::onMouseMove,
                        )
                        .handleMouseZoom(
                            viewportManager = editorController.viewportManager,
                        )
                        .handleMouseDrag(
                            keyboardInputManager = editorController.keyboardInputManager,
                            viewportManager = editorController.viewportManager,
                            getSelectedActor = editorController::getSelectedActor,
                            getMouseSceneOffset = editorController::getMouseWorldCoordinates,
                            notifySelectedInstanceUpdate = editorController::notifySelectedActorUpdate,
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    kubriko = editorController.kubriko,
                )
                InstanceManagerColumn(
                    registeredTypeIds = editorController.serializationManager.registeredTypeIds.toList(),
                    selectedTypeId = editorController.selectedTypeId.collectAsState().value,
                    selectedUpdatableInstance = editorController.selectedUpdatableActor.collectAsState().value,
                    selectTypeId = editorController::selectActor,
                    resolveTypeId = editorController.serializationManager::getTypeId,
                    deselectSelectedInstance = editorController::deselectSelectedActor,
                    locateSelectedInstance = editorController::locateSelectedActor,
                    deleteSelectedInstance = editorController::removeSelectedActor,
                    notifySelectedInstanceUpdate = editorController::notifySelectedActorUpdate,
                )
            }
            MetadataRow(
                totalActorCount = editorController.totalActorCount.collectAsState().value,
                mouseSceneOffset = editorController.mouseSceneOffset.collectAsState().value,
            )
        }
    }
}