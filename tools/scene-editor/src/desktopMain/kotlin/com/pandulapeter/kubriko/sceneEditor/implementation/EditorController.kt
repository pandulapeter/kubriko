/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.extensions.isWithin
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
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.UserPreferences
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.loadFile
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.saveFile
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class EditorController(
    val kubriko: Kubriko,
    val sceneEditorMode: SceneEditorMode,
    defaultSceneFilename: String?,
    defaultSceneFolderPath: String,
    private val onCloseRequest: () -> Unit,
) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    private val actorManager = kubriko.get<ActorManager>()
    val viewportManager = kubriko.get<ViewportManager>()
    val keyboardInputManager = kubriko.get<KeyboardInputManager>()
    val serializationManager = kubriko.get<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val userPreferences = UserPreferences(kubriko.get())
    private val editorActors = listOf(
        GridOverlay(viewportManager),
        KeyboardInputListener(viewportManager, ::navigateBack),
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
    private val _selectedTypeId = MutableStateFlow<String?>(null)
    val selectedTypeId = _selectedTypeId.asStateFlow()
    val colorEditorMode = userPreferences.colorEditorMode
    val angleEditorMode = userPreferences.angleEditorMode
    val isDebugMenuEnabled = userPreferences.isDebugMenuEnabled
    private val _currentFolderPath = MutableStateFlow(defaultSceneFolderPath)
    val currentFolderPath = _currentFolderPath.asStateFlow()
    private val _currentFileName = MutableStateFlow(defaultSceneFilename ?: DEFAULT_SCENE_FILE_NAME)
    val currentFileName = _currentFileName.asStateFlow()
    private val _shouldShowVisibleOnly = MutableStateFlow(true)
    val shouldShowVisibleOnly = _shouldShowVisibleOnly.asStateFlow()
    private val _shouldShowLoadingIndicator = MutableStateFlow(false)
    val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()
    var previewOverlayActor: Editable<*>? = null

    init {
        actorManager.add(editorActors)
        when (sceneEditorMode) {
            SceneEditorMode.Normal -> {
                defaultSceneFilename?.let { loadMap("${currentFolderPath.value}/$it") }
            }

            is SceneEditorMode.Connected -> {
                parseJson(
                    json = sceneEditorMode.sceneJson,
                )
            }
        }
    }

    fun onShouldShowVisibleOnlyToggled() = _shouldShowVisibleOnly.update { currentValue ->
        !currentValue
    }

    fun getSelectedActor() = selectedUpdatableActor.value.first

    fun getMouseWorldCoordinates() = mouseSceneOffset.value

    fun onLeftClick(screenCoordinates: Offset) {
        val positionInWorld = screenCoordinates.toSceneOffset(viewportManager)
        findActorOnPosition(positionInWorld).let { actorAtPosition ->
            selectedUpdatableActor.value.first.let { currentSelectedActor ->
                if (actorAtPosition == null) {
                    if (currentSelectedActor == null) {
                        previewOverlayActor?.let {
                            actorManager.add(it)
                            selectActor(it)
                            previewOverlayActor = null
                        }
                    } else {
                        deselectSelectedActor()
                    }
                } else {
                    (actorAtPosition as? Editable<*>)?.let(::selectActor)
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
                    actorManager.remove(actorAtPosition)
                }
            }
        }
    }

    private fun findActorOnPosition(sceneOffset: SceneOffset) = filteredVisibleActorsWithinViewport.value
        .filter { sceneOffset.isWithin(it.body) }
        .minByOrNull { (it as? Visible)?.drawingOrder ?: 0f }

    fun selectActor(actor: Editable<*>) = _selectedActor.update {
        if (selectedUpdatableActor.value.first == actor) {
            null
        } else {
            actor
        }
    }

    fun removeSelectedActor() = _selectedActor.update { selectedActor ->
        selectedActor?.let { actorManager.remove(selectedActor) }
        null
    }

    fun onMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateSelectedActor() {
        (_selectedActor.value as? Visible)?.let { visibleTrait ->
            viewportManager.setCameraPosition(visibleTrait.body.position)
        }
    }

    fun notifySelectedActorUpdate() = triggerActorUpdate.update { !it }

    fun onColorEditorModeChanged(colorEditorMode: ColorEditorMode) = userPreferences.colorEditorMode.update { colorEditorMode }

    fun onAngleEditorModeChanged(angleEditorMode: AngleEditorMode) = userPreferences.angleEditorMode.update { angleEditorMode }

    fun onIsDebugMenuEnabledChanged(isDebugMenuEnabled: Boolean) = userPreferences.isDebugMenuEnabled.update { isDebugMenuEnabled }

    fun onFilterTextChanged(filterText: String) = _filterText.update { filterText }

    fun selectActorType(typeId: String?) = _selectedTypeId.update { currentValue -> if (currentValue == typeId) null else typeId }

    fun deselectSelectedActor() = _selectedActor.update { null }

    fun reset() {
        viewportManager.setCameraPosition(SceneOffset.Zero)
        _currentFileName.update { DEFAULT_SCENE_FILE_NAME }
        _selectedActor.update { null }
        actorManager.removeAll()
        actorManager.add(editorActors)
    }

    fun loadMap(path: String) {
        _shouldShowLoadingIndicator.update { true }
        launch {
            loadFile(path)?.let { json ->
                parseJson(json)
                updateCurrentFolderPathAndFileName(path)
                _shouldShowLoadingIndicator.update { false }
            }
        }
    }

    private fun parseJson(json: String) {
        val actors = serializationManager.deserializeActors(json)
        actorManager.removeAll()
        actorManager.add(actors + editorActors)
        _selectedActor.update { null }
    }

    fun syncScene() {
        launch {
            (sceneEditorMode as? SceneEditorMode.Connected)?.onSceneJsonChanged?.invoke(serializationManager.serializeActors(allEditableActors.value))
        }
    }

    fun saveScene(path: String) {
        launch {
            saveFile(
                path = path,
                content = serializationManager.serializeActors(allEditableActors.value),
            )
            updateCurrentFolderPathAndFileName(path)
        }
    }

    private fun updateCurrentFolderPathAndFileName(path: String) {
        _currentFolderPath.update { path.split('/').let { it.take(it.size - 1) }.joinToString("/") }
        _currentFileName.update { path.split('/').last() }
    }

    private fun navigateBack() {
        if (selectedUpdatableActor.value.first == null) {
            if (selectedTypeId.value == null) {
                onCloseRequest()
            } else {
                selectActorType(null)
            }
        } else {
            deselectSelectedActor()
        }
    }

    companion object {
        private const val DEFAULT_SCENE_FILE_NAME = "scene_untitled.json"
    }
}