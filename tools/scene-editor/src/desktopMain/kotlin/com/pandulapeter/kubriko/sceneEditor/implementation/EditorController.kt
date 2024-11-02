package com.pandulapeter.kubriko.sceneEditor.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.occupiesPosition
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.sceneEditor.implementation.actors.GridOverlay
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.exitApp
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeyReleased
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeys
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.loadFile
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.saveFile
import com.pandulapeter.kubriko.sceneSerializer.Editable
import com.pandulapeter.kubriko.sceneSerializer.SceneSerializer
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class EditorController(
    val kubriko: Kubriko,
    val sceneSerializer: SceneSerializer,
) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    private val editorActors = listOf(
        GridOverlay(kubriko.viewportManager),
    )
    val allEditableActors = kubriko.actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val visibleActorsWithinViewport = kubriko.actorManager.visibleActorsWithinViewport
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val totalActorCount = kubriko.actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>().count() }
        .stateIn(this, SharingStarted.Eagerly, 0)
    private val mouseScreenCoordinates = MutableStateFlow(Offset.Zero)
    val mouseSceneOffset = combine(
        mouseScreenCoordinates,
        kubriko.viewportManager.cameraPosition,
        kubriko.viewportManager.size,
        kubriko.viewportManager.scaleFactor,
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

    init {
        kubriko.isEditor = true
        kubriko.inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(kubriko.viewportManager::handleKeys)
            .launchIn(this)
        kubriko.inputManager.onKeyReleased
            .onEach { key ->
                handleKeyReleased(
                    key = key,
                    onNavigateBackRequested = ::navigateBack,
                )
            }
            .launchIn(this)
    }

    fun onShouldShowVisibleOnlyToggled() = _shouldShowVisibleOnly.update { currentValue ->
        !currentValue
    }

    fun getSelectedActor() = selectedUpdatableActor.value.first

    fun getMouseWorldCoordinates() = mouseSceneOffset.value

    fun onLeftClick(screenCoordinates: Offset) {
        val positionInWorld = screenCoordinates.toSceneOffset(kubriko.viewportManager)
        findActorOnPosition(positionInWorld).let { actorAtPosition ->
            selectedUpdatableActor.value.first.let { currentSelectedActor ->
                if (actorAtPosition == null) {
                    if (currentSelectedActor == null) {
                        launch {
                            val typeId = selectedTypeId.value
                            // TODO: Should find a better way
                            sceneSerializer.deserializeActors(
                                serializedStates = "[{\"typeId\":\"$typeId\",\"state\":\"{\\\"position\\\":{\\\"x\\\":${positionInWorld.x.raw},\\\"y\\\":${positionInWorld.y.raw}}}\"}]"
                            ).firstOrNull()?.let { actor ->
                                kubriko.actorManager.add(actor)
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
        findActorOnPosition(screenCoordinates.toSceneOffset(kubriko.viewportManager)).let { actorAtPosition ->
            if (actorAtPosition != null) {
                if (actorAtPosition == _selectedActor.value) {
                    removeSelectedActor()
                } else {
                    kubriko.actorManager.remove(actorAtPosition)
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
        selectedActor?.let { kubriko.actorManager.remove(selectedActor) }
        null
    }

    fun onMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateSelectedActor() {
        (_selectedActor.value as? Visible)?.let { visibleTrait ->
            kubriko.viewportManager.setCameraPosition(visibleTrait.position)
        }
    }

    fun notifySelectedActorUpdate() = triggerActorUpdate.update { !it }

    fun selectActor(typeId: String) = _selectedTypeId.update { typeId }

    fun deselectSelectedActor() {
        // TODO _selectedActor.value?.isSelectedInEditor = false
        _selectedActor.update { null }
    }

    fun reset() {
        kubriko.viewportManager.setCameraPosition(SceneOffset.Zero)
        _currentFileName.update { DEFAULT_SCENE_FILE_NAME }
        _selectedActor.update { null }
        kubriko.actorManager.removeAll()
        kubriko.actorManager.add(actors = editorActors.toTypedArray())
    }

    fun loadMap(path: String) {
        launch {
            loadFile(path)?.let { json ->
                val actors = sceneSerializer.deserializeActors(json)
                kubriko.actorManager.removeAll()
                kubriko.actorManager.add(actors = (actors + editorActors).toTypedArray())
                _currentFileName.update { path.split('/').last() }
            }
        }
    }

    fun saveMap(path: String) {
        launch {
            saveFile(
                path = path,
                content = sceneSerializer.serializeActors(allEditableActors.value),
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