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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.reflect.full.isSubclassOf

internal class EditorController(
    val kubriko: Kubriko,
    val sceneEditorMode: SceneEditorMode,
    defaultSceneFilename: String?,
    defaultSceneFolderPath: String,
    private val isSettingsOpen: () -> Boolean,
    private val onCloseRequest: () -> Unit,
) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    private val actorManager = kubriko.get<ActorManager>()
    val viewportManager = kubriko.get<ViewportManager>()
    val keyboardInputManager = kubriko.get<KeyboardInputManager>()
    val serializationManager = kubriko.get<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val userPreferences = UserPreferences(kubriko.get())
    private val editorActors = listOf(
        GridOverlay(viewportManager, userPreferences),
        KeyboardInputListener(
            viewportManager = viewportManager,
            keyboardInputManager = keyboardInputManager,
            isTextInputFocused = { focusedTextInputCount > 0 },
            navigateBack = ::navigateBack,
            onUndo = ::onUndo,
            onRedo = ::onRedo,
            onInteractionModeSelected = ::setInteractionMode,
        ),
    )
    private val allEditableActors = actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    private val _filterText = MutableStateFlow("")
    val filterText = _filterText.asStateFlow()
    val filteredAllEditableActors = combine(
        allEditableActors,
        filterText,
    ) { allEditableActors, filterText ->
        allEditableActors.filter { serializationManager.getTypeId(it::class)?.contains(filterText, true) == true }
    }.stateIn(this, SharingStarted.Eagerly, emptyList())
    val filteredVisibleActorsWithinViewport = combine(
        actorManager.visibleActorsWithinViewport.map { it.filterIsInstance<Editable<*>>() },
        filterText,
    ) { visibleActorsWithinViewport, filterText ->
        visibleActorsWithinViewport.filter { serializationManager.getTypeId(it::class)?.contains(filterText, true) == true }
    }.stateIn(this, SharingStarted.Eagerly, emptyList())
    val totalActorCount = actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>().count() }
        .stateIn(this, SharingStarted.Eagerly, 0)
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
    }.stateIn(this, SharingStarted.Eagerly, SceneOffset.Zero)
    private val triggerActorUpdate = MutableStateFlow(false)
    private val _selectedActor = MutableStateFlow<Editable<*>?>(null)
    val selectedUpdatableActor = combine(
        _selectedActor,
        triggerActorUpdate,
    ) { actor, triggerActorUpdate ->
        actor to triggerActorUpdate
    }.stateIn(this, SharingStarted.Eagerly, null to false)
    val canLocateSelectedActor = combine(
        _selectedActor,
        viewportManager.cameraPosition,
        triggerActorUpdate,
    ) { selectedActor, cameraPosition, _ ->
        (selectedActor as? Visible)?.let { visibleActor ->
            !cameraPosition.isRoughlyAt(visibleActor.body.position)
        } ?: false
    }.stateIn(this, SharingStarted.Eagerly, false)
    private val _selectedTypeId = MutableStateFlow<String?>(null)
    val selectedTypeId = _selectedTypeId.asStateFlow()
    val colorEditorMode = userPreferences.colorEditorMode
    val angleEditorMode = userPreferences.angleEditorMode
    val isDebugMenuEnabled = userPreferences.isDebugMenuEnabled
    private val _shouldShowVisibleOnly = MutableStateFlow(true)
    val shouldShowVisibleOnly = _shouldShowVisibleOnly.asStateFlow()
    private val _interactionMode = MutableStateFlow(SceneEditorInteractionMode.Translate)
    val interactionMode = _interactionMode.asStateFlow()
    private var _previewOverlayActor: Editable<*>? = null
    val previewOverlayActor get() = _previewOverlayActor
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
        scope = this,
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
        scope = this,
        viewportManager = viewportManager,
    )
    private var focusedTextInputCount = 0
    val snapMode = combine(
        userPreferences.snapX,
        userPreferences.snapY,
    ) { snapX, snapY ->
        snapX to snapY
    }.stateIn(this, SharingStarted.Eagerly, 0 to 0)

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

    fun getSelectedActor() = _selectedActor.value

    fun isPlacingNewInstance() = previewOverlayActor != null && getSelectedActor() == null

    fun getMouseWorldCoordinates() = mouseSceneOffset.value

    fun onLeftClick(screenCoordinates: Offset) {
        val positionInWorld = screenCoordinates.toSceneOffset(viewportManager)
        findActorOnPosition(positionInWorld).let { actorAtPosition ->
            _selectedActor.value.let { currentSelectedActor ->
                if (actorAtPosition == null) {
                    if (currentSelectedActor == null) {
                        previewOverlayActor?.let {
                            sceneDocument.recordSnapshot()
                            sceneDocument.addSceneActor(it)
                            sceneDocument.markSceneAsModified()
                            selectActor(it)
                            _previewOverlayActor = selectedTypeId.value?.let(::instantiatePreview)
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
                if (actorAtPosition == _selectedActor.value) {
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
        _selectedActor.update { currentSelectedActor ->
            if (currentSelectedActor == actor) {
                null
            } else {
                actor
            }
        }
    }

    fun removeSelectedActor() {
        val selectedActor = _selectedActor.value ?: return
        sceneDocument.recordSnapshot()
        sceneDocument.removeSceneActor(selectedActor)
        sceneDocument.markSceneAsModified()
        _selectedActor.value = null
    }

    fun onMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateSelectedActor() {
        (_selectedActor.value as? Visible)?.let { visibleTrait ->
            cameraAnimator.animateCameraTo(visibleTrait.body.position)
        }
    }

    fun notifySelectedActorUpdate() {
        sceneDocument.markSceneAsModified()
        triggerActorUpdate.update { !it }
    }

    fun onColorEditorModeChanged(colorEditorMode: ColorEditorMode) = userPreferences.colorEditorMode.update { colorEditorMode }

    fun onAngleEditorModeChanged(angleEditorMode: AngleEditorMode) = userPreferences.angleEditorMode.update { angleEditorMode }

    fun onIsDebugMenuEnabledChanged(isDebugMenuEnabled: Boolean) = userPreferences.isDebugMenuEnabled.update { isDebugMenuEnabled }

    fun onFilterTextChanged(filterText: String) = _filterText.update { filterText }

    fun selectActorType(typeId: String?) {
        _selectedTypeId.update { currentValue -> if (currentValue == typeId) null else typeId }
        _previewOverlayActor = selectedTypeId.value?.let(::instantiatePreview)
    }

    private fun instantiatePreview(typeId: String) = serializationManager.getMetadata(typeId)?.instantiate?.invoke(SceneOffset.Zero)?.restore()

    fun isTypeUnique(typeId: String) = serializationManager.getMetadata(typeId)?.type?.isSubclassOf(Unique::class) == true

    fun deselectSelectedActor() {
        sceneDocument.clearPendingPropertyEdit()
        _selectedActor.update { null }
    }

    fun onUndo() = sceneDocument.undo(_selectedActor.value) { restoredSelection -> _selectedActor.update { restoredSelection } }

    fun onRedo() = sceneDocument.redo(_selectedActor.value) { restoredSelection -> _selectedActor.update { restoredSelection } }

    fun onBeforePropertyChange(editKey: Any) = sceneDocument.onBeforePropertyChange(editKey)

    fun onBeforeActorDrag() = sceneDocument.recordSnapshot()

    fun dispose() {
        cameraAnimator.cancel()
        cancel()
    }

    fun reset() {
        cameraAnimator.cancel()
        viewportManager.setCameraPosition(SceneOffset.Zero)
        sceneFiles.resetFileName()
        _selectedActor.update { null }
        sceneDocument.clearSceneActors()
        sceneDocument.onSceneReplaced()
    }

    fun loadMap(path: String) = sceneFiles.loadMap(path) { actors ->
        sceneDocument.replaceSceneActors(actors)
        _selectedActor.update { null }
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
