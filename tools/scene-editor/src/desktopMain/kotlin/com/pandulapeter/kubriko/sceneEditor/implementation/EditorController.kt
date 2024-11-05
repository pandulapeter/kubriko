package com.pandulapeter.kubriko.sceneEditor.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.occupiesPosition
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.sceneEditor.implementation.actors.GridOverlay
import com.pandulapeter.kubriko.sceneEditor.implementation.actors.KeyboardInputListener
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.exitApp
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.loadFile
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.saveFile
import com.pandulapeter.kubriko.serializationManager.SerializationManager
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
) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    private val actorManager = kubriko.require<ActorManager>()
    val viewportManager = kubriko.require<ViewportManager>()
    val keyboardInputManager = kubriko.require<KeyboardInputManager>()
    val serializationManager = kubriko.require<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val editorActors = listOf(
        GridOverlay(viewportManager),
        KeyboardInputListener(viewportManager, ::navigateBack),
    )
    val allEditableActors = actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val visibleActorsWithinViewport = actorManager.visibleActorsWithinViewport
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val totalActorCount = actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>().count() }
        .stateIn(this, SharingStarted.Eagerly, 0)
    private val mouseScreenCoordinates = MutableStateFlow(Offset.Zero)
    val mouseSceneOffset = combine(
        mouseScreenCoordinates,
        viewportManager.cameraPosition,
        viewportManager.size,
        viewportManager.scaleFactor,
    ) { mouseScreenCoordinates, viewportCenter, viewportSize, viewportScaleFactor ->
        mouseScreenCoordinates.toSceneOffset(
            viewportCenter = viewportCenter,
            viewportSize = viewportSize,
            viewportScaleFactor = viewportScaleFactor,
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
    private val _currentFileName = MutableStateFlow(DEFAULT_SCENE_FILE_NAME)
    val currentFileName = _currentFileName.asStateFlow()
    private val _shouldShowVisibleOnly = MutableStateFlow(false)
    val shouldShowVisibleOnly = _shouldShowVisibleOnly.asStateFlow()

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
                        selectedTypeId.value?.let { typeId ->
                            serializationManager.getMetadata(typeId)?.instantiate?.invoke(positionInWorld)?.restore()?.let {
                                actorManager.add(it)
                            }
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

    private fun findActorOnPosition(sceneOffset: SceneOffset) = visibleActorsWithinViewport.value
        .filter { it.occupiesPosition(sceneOffset) }
        .minByOrNull { (it as? Visible)?.drawingOrder ?: 0f }

    fun selectActor(actor: Editable<*>) {
        val currentSelectedActor = selectedUpdatableActor.value.first
        // TODO currentSelectedActor?.isSelectedInEditor = false
        _selectedActor.update {
            if (currentSelectedActor == actor) {
                null
            } else {
                actor// TODO .also { it.isSelectedInEditor = true }
            }
        }
    }

    fun removeSelectedActor() = _selectedActor.update { selectedActor ->
        selectedActor?.let { actorManager.remove(selectedActor) }
        null
    }

    fun onMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateSelectedActor() {
        (_selectedActor.value as? Visible)?.let { visibleTrait ->
            viewportManager.setCameraPosition(visibleTrait.position)
        }
    }

    fun notifySelectedActorUpdate() = triggerActorUpdate.update { !it }

    fun selectActor(typeId: String) = _selectedTypeId.update { typeId }

    fun deselectSelectedActor() {
        // TODO _selectedActor.value?.isSelectedInEditor = false
        _selectedActor.update { null }
    }

    fun reset() {
        viewportManager.setCameraPosition(SceneOffset.Zero)
        _currentFileName.update { DEFAULT_SCENE_FILE_NAME }
        _selectedActor.update { null }
        actorManager.removeAll()
        actorManager.add(actors = editorActors.toTypedArray())
    }

    fun loadMap(path: String) {
        launch {
            loadFile(path)?.let { json ->
                val actors = serializationManager.deserializeActors(json)
                actorManager.removeAll()
                actorManager.add(actors = (actors + editorActors).toTypedArray())
                _currentFileName.update { path.split('/').last() }
            }
        }
    }

    fun saveMap(path: String) {
        launch {
            saveFile(
                path = path,
                content = serializationManager.serializeActors(allEditableActors.value),
            )
        }
    }

    private fun navigateBack() {
        if (selectedUpdatableActor.value.first == null) {
            exitApp()
        } else {
            deselectSelectedActor()
        }
    }

    companion object {
        const val SCENES_DIRECTORY = "./src/commonMain/composeResources/files/scenes"
        private const val DEFAULT_SCENE_FILE_NAME = "scene_untitled.json"
    }
}