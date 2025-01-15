package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.debugMenu.KubrikoViewportWithDebugMenu
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.EditorController
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.handleMouseClick
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.handleMouseDrag
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.handleMouseMove
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.handleMouseZoom
import com.pandulapeter.kubriko.sceneEditor.implementation.overlay.EditorOverlay
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.fileManagerRow.FileManagerRow
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceBrowserColumn.InstanceBrowserColumn
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.InstanceManagerColumn
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.metadataRow.MetadataRow
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

private val fileManagerRowHeight = 32.dp
private val metadataRowHeight = 32.dp
private val instanceBrowserColumnWidth = 150.dp
private val instanceManagerColumnWidth = 220.dp

@Composable
internal fun EditorUserInterface(
    modifier: Modifier = Modifier,
    editorController: EditorController,
    openFilePickerForLoading: () -> Unit,
    openFilePickerForSaving: () -> Unit,
    openSettings: () -> Unit,
    overlayKubriko: Kubriko,
) = KubrikoTheme {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxWidth().padding(paddingValues),
        ) {
            Column {
                Spacer(
                    modifier = Modifier.height(fileManagerRowHeight),
                )
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Spacer(
                            modifier = Modifier.fillMaxHeight().width(instanceBrowserColumnWidth),
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                        ) {
                            KubrikoViewportWithDebugMenu(
                                kubriko = editorController.kubriko,
                                isEnabled = editorController.isDebugMenuEnabled.collectAsState().value,
                            ) {
                                KubrikoViewport(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    kubriko = editorController.kubriko,
                                )
                                EditorOverlay(
                                    // TODO: Migrate to PonterInputAware
                                    modifier = Modifier
                                        .fillMaxSize()
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
                                        ),
                                    editorController = editorController,
                                    overlayKubriko = overlayKubriko,
                                )
                            }
                        }
                        InstanceManagerColumn(
                            modifier = Modifier.fillMaxHeight().width(instanceManagerColumnWidth),
                            registeredTypeIds = editorController.serializationManager.registeredTypeIds.toList(),
                            selectedTypeId = editorController.selectedTypeId.collectAsState().value,
                            selectedUpdatableInstance = editorController.selectedUpdatableActor.collectAsState().value,
                            selectTypeId = editorController::selectActorType,
                            resolveTypeId = editorController.serializationManager::getTypeId,
                            deselectSelectedInstance = editorController::deselectSelectedActor,
                            locateSelectedInstance = editorController::locateSelectedActor,
                            deleteSelectedInstance = editorController::removeSelectedActor,
                            notifySelectedInstanceUpdate = editorController::notifySelectedActorUpdate,
                            colorEditorMode = editorController.colorEditorMode.collectAsState().value,
                            angleEditorMode = editorController.angleEditorMode.collectAsState().value,
                        )
                    }
                    InstanceBrowserColumn(
                        modifier = Modifier.fillMaxHeight().width(instanceBrowserColumnWidth),
                        filterText = editorController.filterText.collectAsState().value,
                        onFilterTextChanged = editorController::onFilterTextChanged,
                        shouldShowVisibleOnly = editorController.shouldShowVisibleOnly.collectAsState().value,
                        allInstances = editorController.filteredAllEditableActors.collectAsState().value,
                        visibleInstances = editorController.filteredVisibleActorsWithinViewport.collectAsState().value,
                        selectedUpdatableInstance = editorController.selectedUpdatableActor.collectAsState().value,
                        onShouldShowVisibleOnlyToggled = editorController::onShouldShowVisibleOnlyToggled,
                        selectInstance = editorController::selectActor,
                        resolveTypeId = editorController.serializationManager::getTypeId,
                    )
                }
                MetadataRow(
                    modifier = Modifier.height(metadataRowHeight),
                    totalActorCount = editorController.totalActorCount.collectAsState().value,
                    mouseSceneOffset = editorController.mouseSceneOffset.collectAsState().value,
                )
            }
            FileManagerRow(
                modifier = Modifier.height(fileManagerRowHeight),
                isConnected = editorController.sceneEditorMode is SceneEditorMode.Connected,
                currentFileName = editorController.currentFileName.collectAsState().value,
                onNewIconClicked = editorController::reset,
                onOpenIconClicked = openFilePickerForLoading,
                onSaveIconClicked = openFilePickerForSaving,
                onSyncIconClicked = editorController::syncScene,
                onSettingsIconClicked = openSettings,
            )
        }
    }
}