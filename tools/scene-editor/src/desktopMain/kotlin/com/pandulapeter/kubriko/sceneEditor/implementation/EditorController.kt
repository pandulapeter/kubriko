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
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.UndoRedoHistory
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.deserializeSceneOrNull
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.indexOfReplacedUnique
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.UserPreferences
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.loadFile
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.navigateBackAction
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.restoredSelectionIndex
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.saveFile
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.IdentityHashMap
import kotlin.math.roundToInt
import kotlin.reflect.full.isSubclassOf
import kotlin.time.TimeSource

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
    private val _currentFolderPath = MutableStateFlow(defaultSceneFolderPath)
    val currentFolderPath = _currentFolderPath.asStateFlow()
    private val _currentFileName = MutableStateFlow(defaultSceneFilename ?: DEFAULT_SCENE_FILE_NAME)
    val currentFileName = _currentFileName.asStateFlow()
    private val _shouldShowVisibleOnly = MutableStateFlow(true)
    val shouldShowVisibleOnly = _shouldShowVisibleOnly.asStateFlow()
    private val _shouldShowLoadingIndicator = MutableStateFlow(false)
    val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()
    private val _interactionMode = MutableStateFlow(SceneEditorInteractionMode.Translate)
    val interactionMode = _interactionMode.asStateFlow()
    var previewOverlayActor: Editable<*>? = null
    private val undoRedoHistory = UndoRedoHistory()
    val canUndo = undoRedoHistory.canUndo
    val canRedo = undoRedoHistory.canRedo
    private val _isSceneModified = MutableStateFlow(false)
    val isSceneModified = _isSceneModified.asStateFlow()
    private var pendingPropertyEditKey: Any? = null
    private var cameraAnimationJob: Job? = null
    private var focusedTextInputCount = 0
    private val sceneActors = mutableListOf<Editable<*>>()
    private val sceneActorIds = IdentityHashMap<Editable<*>, Long>()
    private var nextSceneActorId = 0L
    private val _fileOperationError = MutableStateFlow<Pair<FileOperationError, String>?>(null)
    val fileOperationError = _fileOperationError.asStateFlow()
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
                val sceneJson = sceneEditorMode.sceneJson
                if (sceneJson.isNotBlank()) {
                    val actors = deserializeSceneOrNull(sceneJson, serializationManager::deserializeActors)
                    if (actors == null) {
                        _fileOperationError.update { FileOperationError.CONNECTED_SCENE_INVALID to "" }
                    } else {
                        replaceSceneActors(actors)
                    }
                }
                onSceneReplaced()
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
                            recordSnapshot()
                            addSceneActor(it)
                            markSceneAsModified()
                            selectActor(it)
                            previewOverlayActor = null
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
                    recordSnapshot()
                    removeSceneActor(actorAtPosition)
                    markSceneAsModified()
                }
            }
        }
    }

    private fun findActorOnPosition(sceneOffset: SceneOffset) = filteredVisibleActorsWithinViewport.value
        .filter { sceneOffset.isCollidingWith(it.body.boundingBoxCollisionMask) }
        .minByOrNull { (it as? Visible)?.drawingOrder ?: 0f }

    fun selectActor(actor: Editable<*>) {
        pendingPropertyEditKey = null
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
        recordSnapshot()
        removeSceneActor(selectedActor)
        markSceneAsModified()
        _selectedActor.value = null
    }

    fun onMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateSelectedActor() {
        (_selectedActor.value as? Visible)?.let { visibleTrait ->
            animateCameraTo(visibleTrait.body.position)
        }
    }

    private fun animateCameraTo(target: SceneOffset) {
        val start = viewportManager.cameraPosition.value
        val delta = target - start
        cameraAnimationJob?.cancel()
        cameraAnimationJob = launch {
            val startMark = TimeSource.Monotonic.markNow()
            var lastAnimatedPosition = start
            while (isActive) {
                // Any camera movement the animation did not perform means the user took over; yield to them.
                if (viewportManager.cameraPosition.value != lastAnimatedPosition) {
                    return@launch
                }
                val progress = (startMark.elapsedNow().inWholeMilliseconds.toFloat() / CAMERA_ANIMATION_DURATION_MS).coerceIn(0f, 1f)
                lastAnimatedPosition = start + delta * easeInOut(progress)
                viewportManager.setCameraPosition(lastAnimatedPosition)
                if (progress >= 1f) {
                    break
                }
                delay(CAMERA_ANIMATION_FRAME_DELAY_MS)
            }
        }
    }

    fun notifySelectedActorUpdate() {
        markSceneAsModified()
        triggerActorUpdate.update { !it }
    }

    fun onColorEditorModeChanged(colorEditorMode: ColorEditorMode) = userPreferences.colorEditorMode.update { colorEditorMode }

    fun onAngleEditorModeChanged(angleEditorMode: AngleEditorMode) = userPreferences.angleEditorMode.update { angleEditorMode }

    fun onIsDebugMenuEnabledChanged(isDebugMenuEnabled: Boolean) = userPreferences.isDebugMenuEnabled.update { isDebugMenuEnabled }

    fun onFilterTextChanged(filterText: String) = _filterText.update { filterText }

    fun selectActorType(typeId: String?) = _selectedTypeId.update { currentValue -> if (currentValue == typeId) null else typeId }

    fun isTypeUnique(typeId: String) = serializationManager.getMetadata(typeId)?.type?.isSubclassOf(Unique::class) == true

    fun deselectSelectedActor() {
        pendingPropertyEditKey = null
        _selectedActor.update { null }
    }

    fun onUndo() {
        undoRedoHistory.performUndo(takeSnapshot())?.let(::restoreSnapshot)
        pendingPropertyEditKey = null
    }

    fun onRedo() {
        undoRedoHistory.performRedo(takeSnapshot())?.let(::restoreSnapshot)
        pendingPropertyEditKey = null
    }

    /**
     * Records the pre-change state of the scene before a property of the selected actor is edited.
     * Consecutive edits sharing the same [editKey] are coalesced into a single undo step, so dragging a
     * slider or typing into a field does not flood the history.
     */
    fun onBeforePropertyChange(editKey: Any) {
        if (editKey != pendingPropertyEditKey) {
            undoRedoHistory.recordAction(takeSnapshot())
            pendingPropertyEditKey = editKey
        }
    }

    fun onBeforeActorDrag() = recordSnapshot()

    private fun recordSnapshot() {
        undoRedoHistory.recordAction(takeSnapshot())
        pendingPropertyEditKey = null
    }

    private fun takeSnapshot() = UndoRedoHistory.SceneSnapshot(
        serializedScene = serializationManager.serializeActors(sceneActors),
        isSceneModified = _isSceneModified.value,
        actorIds = LongArray(sceneActors.size) { sceneActorIds.getValue(sceneActors[it]) },
    )

    private fun restoreSnapshot(snapshot: UndoRedoHistory.SceneSnapshot) {
        val selectedId = _selectedActor.value?.let(sceneActorIds::get)
        val restoredActors = serializationManager.deserializeActors(snapshot.serializedScene)
        val ids = snapshot.actorIds.takeIf { it.size == restoredActors.size }
        clearSceneActors()
        restoredActors.forEachIndexed { index, actor -> trackSceneActor(actor, ids?.get(index) ?: nextSceneActorId++) }
        actorManager.add(restoredActors)
        _selectedActor.update {
            restoredSelectionIndex(selectedId, snapshot.actorIds, restoredActors.size)?.let(restoredActors::get)
        }
        _isSceneModified.update { snapshot.isSceneModified }
    }

    private fun markSceneAsModified() = _isSceneModified.update { true }

    private fun onSceneReplaced() {
        undoRedoHistory.reset()
        _isSceneModified.update { false }
        pendingPropertyEditKey = null
    }

    fun dispose() {
        cameraAnimationJob?.cancel()
        cancel()
    }

    fun reset() {
        cameraAnimationJob?.cancel()
        viewportManager.setCameraPosition(SceneOffset.Zero)
        _currentFileName.update { DEFAULT_SCENE_FILE_NAME }
        _selectedActor.update { null }
        clearSceneActors()
        onSceneReplaced()
    }

    fun loadMap(path: String) {
        _shouldShowLoadingIndicator.update { true }
        launch(Dispatchers.Main) {
            try {
                val json = loadFile(path)
                val actors = withContext(Dispatchers.Default) {
                    deserializeSceneOrNull(json, serializationManager::deserializeActors)
                }
                if (actors == null) {
                    reportFileOperationError(FileOperationError.LOAD_FAILED, path)
                } else {
                    replaceSceneActors(actors)
                    _selectedActor.update { null }
                    onSceneReplaced()
                    updateCurrentFolderPathAndFileName(path)
                }
            } catch (_: IOException) {
                reportFileOperationError(FileOperationError.LOAD_FAILED, path)
            } finally {
                _shouldShowLoadingIndicator.update { false }
            }
        }
    }

    fun onFileOperationErrorShown() = _fileOperationError.update { null }

    private fun reportFileOperationError(error: FileOperationError, path: String) = _fileOperationError.update { error to path.split('/').last() }

    private fun replaceSceneActors(actors: List<Editable<*>>) {
        clearSceneActors()
        actors.forEach { trackSceneActor(it) }
        actorManager.add(actors)
    }

    private fun addSceneActor(actor: Editable<*>) {
        trackSceneActor(actor)
        actorManager.add(actor)
    }

    private fun removeSceneActor(actor: Editable<*>) {
        sceneActors.remove(actor)
        sceneActorIds.remove(actor)
        actorManager.remove(actor)
    }

    /**
     * Records [actor] as part of the scene. The scene is tracked here rather than read back from [ActorManager],
     * whose batched updates lag behind the editor's own actions. A [Unique] actor replaces the tracked one of the
     * same class, mirroring what [ActorManager] does with the actors themselves.
     */
    private fun trackSceneActor(actor: Editable<*>, id: Long = nextSceneActorId++) {
        val replacedIndex = indexOfReplacedUnique(sceneActors.map { it::class }, actor::class, actor is Unique)
        if (replacedIndex >= 0) {
            sceneActorIds.remove(sceneActors.removeAt(replacedIndex))
        }
        sceneActors.add(actor)
        sceneActorIds[actor] = id
    }

    private fun clearSceneActors() {
        actorManager.remove(sceneActors.toList())
        sceneActors.clear()
        sceneActorIds.clear()
    }

    fun syncScene() {
        val onSceneJsonChanged = (sceneEditorMode as? SceneEditorMode.Connected)?.onSceneJsonChanged ?: return
        val content = serializationManager.serializeActors(sceneActors)
        launch {
            onSceneJsonChanged(content)
        }
    }

    fun saveScene(path: String) {
        val content = serializationManager.serializeActors(sceneActors)
        launch(Dispatchers.Main) {
            try {
                saveFile(
                    path = path,
                    content = content,
                )
                updateCurrentFolderPathAndFileName(path)
                _isSceneModified.update { false }
                pendingPropertyEditKey = null
            } catch (_: IOException) {
                reportFileOperationError(FileOperationError.SAVE_FAILED, path)
            }
        }
    }

    private fun updateCurrentFolderPathAndFileName(path: String) {
        _currentFolderPath.update { path.split('/').let { it.take(it.size - 1) }.joinToString("/") }
        _currentFileName.update { path.split('/').last() }
    }

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

    companion object {
        private const val DEFAULT_SCENE_FILE_NAME = "scene_untitled.json"
        private const val CAMERA_ANIMATION_DURATION_MS = 350f
        private const val CAMERA_ANIMATION_FRAME_DELAY_MS = 8L

        private fun easeInOut(progress: Float) = progress * progress * (3f - 2f * progress)

        private fun SceneOffset.isRoughlyAt(other: SceneOffset) =
            raw.x.roundToInt() == other.raw.x.roundToInt() && raw.y.roundToInt() == other.raw.y.roundToInt()
    }
}
