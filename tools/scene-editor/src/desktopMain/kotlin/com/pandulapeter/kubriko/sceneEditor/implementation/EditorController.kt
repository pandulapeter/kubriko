/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.extensions.isCollidingWith
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.toSceneOffset
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.actors.GridOverlay
import com.pandulapeter.kubriko.sceneEditor.implementation.actors.KeyboardInputListener
import com.pandulapeter.kubriko.sceneEditor.implementation.extensions.boundingBoxCollisionMask
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.NavigateBackAction
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.UserPreferences
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.navigateBackAction
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.reflect.full.isSubclassOf

internal class EditorController(
    private val scope: CoroutineScope,
    val kubriko: Kubriko,
    val sceneEditorMode: SceneEditorMode,
    defaultSceneFilename: String?,
    defaultSceneFolderPath: String,
    private val isSettingsOpen: () -> Boolean,
    private val onCloseRequest: () -> Unit,
) {
    private val actorManager = kubriko.get<ActorManager>()
    val viewportManager = kubriko.get<ViewportManager>()
    val keyboardInputManager = kubriko.get<KeyboardInputManager>()
    val serializationManager = kubriko.get<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val userPreferences = UserPreferences(kubriko.get())
    private val editorActors = listOf(
        GridOverlay(viewportManager, userPreferences),
        KeyboardInputListener(
            viewportManager = viewportManager,
            isKeyPressed = keyboardInputManager::isKeyPressed,
            isTextInputFocused = { focusedTextInputCount > 0 },
            navigateBack = ::navigateBack,
            onUndo = ::onUndo,
            onRedo = ::onRedo,
            onInteractionModeSelected = ::setInteractionMode,
        ),
    )
    private val allEditableActors = actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val _filterText = MutableStateFlow("")
    val filterText = _filterText.asStateFlow()
    val filteredAllEditableActors = combine(
        allEditableActors,
        filterText,
    ) { allEditableActors, filterText ->
        allEditableActors.filter { serializationManager.getTypeId(it::class)?.contains(filterText, true) == true }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val filteredVisibleActorsWithinViewport = combine(
        actorManager.visibleActorsWithinViewport.map { it.filterIsInstance<Editable<*>>() },
        filterText,
    ) { visibleActorsWithinViewport, filterText ->
        visibleActorsWithinViewport.filter { serializationManager.getTypeId(it::class)?.contains(filterText, true) == true }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val totalActorCount = actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>().count() }
        .stateIn(scope, SharingStarted.Eagerly, 0)
    private val mouseScreenCoordinates = MutableStateFlow(Offset.Zero)
    val mouseSceneOffset = combine(
        mouseScreenCoordinates,
        viewportManager.cameraPosition,
        viewportManager.size,
    ) { mouseScreenCoordinates, viewportCenter, viewportSize ->
        mouseScreenCoordinates.toSceneOffset(
            viewportCenter = viewportCenter,
            viewportSize = viewportSize,
            viewportScaleFactor = viewportManager.scaleFactor.value,
        )
    }.stateIn(scope, SharingStarted.Eagerly, SceneOffset.Zero)
    private val selection = EditorSelection(
        scope = scope,
        cameraPosition = viewportManager.cameraPosition,
        instantiatePreview = ::instantiatePreview,
    )
    val selectedActor = selection.selectedActor
    val selectedActorRevision = selection.selectedActorRevision
    val canLocateSelectedActor = selection.canLocateSelectedActor
    val selectedTypeId = selection.selectedTypeId
    val colorEditorMode = userPreferences.colorEditorMode
    val angleEditorMode = userPreferences.angleEditorMode
    val isDebugMenuEnabled = userPreferences.isDebugMenuEnabled
    private val _shouldShowVisibleOnly = MutableStateFlow(true)
    val shouldShowVisibleOnly = _shouldShowVisibleOnly.asStateFlow()
    private val _interactionMode = MutableStateFlow(SceneEditorInteractionMode.Translate)
    val interactionMode = _interactionMode.asStateFlow()
    val previewOverlayActor get() = selection.previewOverlayActor
    private val sceneDocument = SceneDocument(
        serialize = serializationManager::serializeActors,
        deserialize = serializationManager::deserializeActors,
        addActors = actorManager::add,
        removeActors = actorManager::remove,
    )
    val canUndo = sceneDocument.canUndo
    val canRedo = sceneDocument.canRedo
    val isSceneModified = sceneDocument.isSceneModified
    private val sceneFiles = SceneFiles(
        scope = scope,
        sceneEditorMode = sceneEditorMode,
        defaultSceneFilename = defaultSceneFilename,
        defaultSceneFolderPath = defaultSceneFolderPath,
        serializeScene = sceneDocument::serializeScene,
        deserializeScene = serializationManager::deserializeActors,
    )
    val currentFolderPath = sceneFiles.currentFolderPath
    val currentFileName = sceneFiles.currentFileName
    val shouldShowLoadingIndicator = sceneFiles.shouldShowLoadingIndicator
    val fileOperationError = sceneFiles.fileOperationError
    private val cameraAnimator = CameraAnimator(
        scope = scope,
        viewportManager = viewportManager,
    )
    private var focusedTextInputCount = 0
    val snapMode = combine(
        userPreferences.snapX,
        userPreferences.snapY,
    ) { snapX, snapY ->
        snapX to snapY
    }.stateIn(scope, SharingStarted.Eagerly, 0 to 0)

    init {
        actorManager.add(editorActors)
        when (sceneEditorMode) {
            SceneEditorMode.Normal -> {
                defaultSceneFilename?.let { loadMap("${currentFolderPath.value}/$it") }
            }

            is SceneEditorMode.Connected -> {
                sceneFiles.readConnectedScene(sceneEditorMode.sceneJson)?.let(sceneDocument::replaceSceneActors)
                sceneDocument.onSceneReplaced()
            }
        }
    }

    fun onSnapModeChanged(snapMode: Pair<Int, Int>) {
        userPreferences.snapX.update { snapMode.first }
        userPreferences.snapY.update { snapMode.second }
    }

    fun onTextInputFocusChanged(isFocused: Boolean) {
        focusedTextInputCount = (focusedTextInputCount + if (isFocused) 1 else -1).coerceAtLeast(0)
    }

    fun onShouldShowVisibleOnlyToggled() = _shouldShowVisibleOnly.update { currentValue ->
        !currentValue
    }

    fun setInteractionMode(interactionMode: SceneEditorInteractionMode) = _interactionMode.update { interactionMode }

    fun getSelectedActor() = selectedActor.value

    fun isPlacingNewInstance() = previewOverlayActor != null && getSelectedActor() == null

    fun getMouseWorldCoordinates() = mouseSceneOffset.value

    fun onLeftClick(screenCoordinates: Offset) {
        val positionInWorld = screenCoordinates.toSceneOffset(viewportManager)
        findActorOnPosition(positionInWorld).let { actorAtPosition ->
            selectedActor.value.let { currentSelectedActor ->
                if (actorAtPosition == null) {
                    if (currentSelectedActor == null) {
                        previewOverlayActor?.let {
                            sceneDocument.recordSnapshot()
                            sceneDocument.addSceneActor(it)
                            sceneDocument.markSceneAsModified()
                            selectActor(it)
                            selection.renewPreview()
                        }
                    } else {
                        deselectSelectedActor()
                    }
                } else {
                    actorAtPosition.let(::selectActor)
                }
            }
        }
    }

    fun onRightClick(screenCoordinates: Offset) {
        findActorOnPosition(screenCoordinates.toSceneOffset(viewportManager)).let { actorAtPosition ->
            if (actorAtPosition != null) {
                if (actorAtPosition == selectedActor.value) {
                    removeSelectedActor()
                } else {
                    sceneDocument.recordSnapshot()
                    sceneDocument.removeSceneActor(actorAtPosition)
                    sceneDocument.markSceneAsModified()
                }
            }
        }
    }

    private fun findActorOnPosition(sceneOffset: SceneOffset) = filteredVisibleActorsWithinViewport.value
        .filter { sceneOffset.isCollidingWith(it.body.boundingBoxCollisionMask) }
        .minByOrNull { (it as? Visible)?.drawingOrder ?: 0f }

    fun selectActor(actor: Editable<*>) {
        sceneDocument.clearPendingPropertyEdit()
        selection.toggleActor(actor)
    }

    fun removeSelectedActor() {
        val actor = selectedActor.value ?: return
        sceneDocument.recordSnapshot()
        sceneDocument.removeSceneActor(actor)
        sceneDocument.markSceneAsModified()
        selection.setSelectedActor(null)
    }

    fun onMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateSelectedActor() {
        (selectedActor.value as? Visible)?.let { visibleTrait ->
            cameraAnimator.animateCameraTo(visibleTrait.body.position)
        }
    }

    fun notifySelectedActorUpdate() {
        sceneDocument.markSceneAsModified()
        selection.notifySelectedActorUpdate()
    }

    fun onColorEditorModeChanged(colorEditorMode: ColorEditorMode) = userPreferences.colorEditorMode.update { colorEditorMode }

    fun onAngleEditorModeChanged(angleEditorMode: AngleEditorMode) = userPreferences.angleEditorMode.update { angleEditorMode }

    fun onIsDebugMenuEnabledChanged(isDebugMenuEnabled: Boolean) = userPreferences.isDebugMenuEnabled.update { isDebugMenuEnabled }

    fun onFilterTextChanged(filterText: String) = _filterText.update { filterText }

    fun selectActorType(typeId: String?) = selection.toggleType(typeId)

    private fun instantiatePreview(typeId: String) = serializationManager.getMetadata(typeId)?.instantiate?.invoke(SceneOffset.Zero)?.restore()

    fun isTypeUnique(typeId: String) = serializationManager.getMetadata(typeId)?.type?.isSubclassOf(Unique::class) == true

    fun deselectSelectedActor() {
        sceneDocument.clearPendingPropertyEdit()
        selection.setSelectedActor(null)
    }

    fun onUndo() = sceneDocument.undo(selectedActor.value, selection::setSelectedActor)

    fun onRedo() = sceneDocument.redo(selectedActor.value, selection::setSelectedActor)

    fun onBeforePropertyChange(editKey: Any) = sceneDocument.onBeforePropertyChange(editKey)

    fun onBeforeActorDrag() = sceneDocument.recordSnapshot()

    fun reset() {
        cameraAnimator.cancel()
        viewportManager.setCameraPosition(SceneOffset.Zero)
        sceneFiles.resetFileName()
        selection.setSelectedActor(null)
        sceneDocument.clearSceneActors()
        sceneDocument.onSceneReplaced()
    }

    fun loadMap(path: String) = sceneFiles.loadMap(path) { actors ->
        sceneDocument.replaceSceneActors(actors)
        selection.setSelectedActor(null)
        sceneDocument.onSceneReplaced()
    }

    fun onFileOperationErrorShown() = sceneFiles.onFileOperationErrorShown()

    fun syncScene() = sceneFiles.syncScene()

    fun saveScene(path: String) = sceneFiles.saveScene(path, sceneDocument::onSceneSaved)

    private fun navigateBack() = when (
        navigateBackAction(
            hasSelectedActor = getSelectedActor() != null,
            hasSelectedType = selectedTypeId.value != null,
            isSettingsOpen = isSettingsOpen(),
            isSceneModified = isSceneModified.value,
            isTextInputFocused = focusedTextInputCount > 0,
        )
    ) {
        NavigateBackAction.DESELECT_ACTOR -> deselectSelectedActor()
        NavigateBackAction.DESELECT_TYPE -> selectActorType(null)
        NavigateBackAction.CLOSE -> onCloseRequest()
        NavigateBackAction.NONE -> Unit
    }
}
