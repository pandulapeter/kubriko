package com.pandulapeter.kubriko.sceneEditor.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubriko.engine.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.engine.traits.Editable
import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.types.SceneOffset
import com.pandulapeter.kubriko.sceneEditor.implementation.actors.GridOverlay
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.exitApp
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeyReleased
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeys
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.loadFile
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.saveFile
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

internal class EditorController(val kubriko: Kubriko) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    private val editorActors = arrayOf(
        GridOverlay(kubriko.viewportManager),
    )
    val allInstances = kubriko.actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val visibleInstancesWithinViewport = kubriko.actorManager.visibleActorsWithinViewport
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val totalActorCount = kubriko.actorManager.allActors
        .map { it.filterIsInstance<Editable<*>>().count() }
        .stateIn(this, SharingStarted.Eagerly, 0)
    private val mouseScreenCoordinates = MutableStateFlow(Offset.Zero)
    val mouseSceneOffset = combine(
        mouseScreenCoordinates,
        kubriko.viewportManager.center,
        kubriko.viewportManager.size,
        kubriko.viewportManager.scaleFactor,
    ) { mouseScreenCoordinates, viewportCenter, viewportSize, viewportScaleFactor ->
        mouseScreenCoordinates.toSceneOffset(
            viewportCenter = viewportCenter,
            viewportSize = viewportSize,
            viewportScaleFactor = viewportScaleFactor,
        )
    }.stateIn(this, SharingStarted.Eagerly, SceneOffset.Zero)
    private val instanceUpdateTrigger = MutableStateFlow(false)
    private val _selectedInstance = MutableStateFlow<Editable<*>?>(null)
    val selectedUpdatableInstance = combine(
        _selectedInstance,
        instanceUpdateTrigger,
    ) { selectedGameObject, triggerGameObjectUpdate ->
        selectedGameObject to triggerGameObjectUpdate
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

    fun getSelectedInstance() = selectedUpdatableInstance.value.first

    fun getMouseWorldCoordinates() = mouseSceneOffset.value

    fun onLeftClick(screenCoordinates: Offset) {
        val positionInWorld = screenCoordinates.toSceneOffset(kubriko.viewportManager)
        findInstanceOnPosition(positionInWorld).let { gameObjectAtPosition ->
            selectedUpdatableInstance.value.first.let { currentSelectedGameObject ->
                if (gameObjectAtPosition == null) {
                    if (currentSelectedGameObject == null) {
                        launch {
                            val typeId = selectedTypeId.value
                            kubriko.serializationManager.deserializeActors(
                                serializedStates = "[{\"typeId\":\"$typeId\",\"state\":\"{\\\"position\\\":{\\\"x\\\":${positionInWorld.x.raw},\\\"y\\\":${positionInWorld.y.raw}}}\"}]"
                            ).firstOrNull()?.let { actor ->
                                kubriko.actorManager.add(actor)
                            }
                        }
                    } else {
                        deselectSelectedInstance()
                    }
                } else {
                    (gameObjectAtPosition as? Editable<*>)?.let { availableInEditor ->
                        selectInstance(gameObjectAtPosition)
                    }
                }
            }
        }
    }

    fun onRightClick(screenCoordinates: Offset) {
        findInstanceOnPosition(screenCoordinates.toSceneOffset(kubriko.viewportManager)).let { actorAtPosition ->
            if (actorAtPosition != null) {
                if (actorAtPosition == _selectedInstance.value) {
                    deleteSelectedInstance()
                } else {
                    kubriko.actorManager.remove(actorAtPosition)
                }
            }
        }
    }

    private fun findInstanceOnPosition(positionInWorld: SceneOffset) = kubriko.actorManager
        .findVisibleInstancesWithBoundsInPosition(positionInWorld)
        .minByOrNull { (it as? Visible)?.drawingOrder ?: 0f }

    fun selectInstance(gameObject: Editable<*>) {
        val currentSelectedGameObject = selectedUpdatableInstance.value.first
        // TODO currentSelectedGameObject?.isSelectedInEditor = false
        _selectedInstance.update {
            if (currentSelectedGameObject == gameObject) {
                null
            } else {
                gameObject// TODO .also { it.isSelectedInEditor = true }
            }
        }
    }

    fun deleteSelectedInstance() {
        _selectedInstance.value?.let { selectedGameObject ->
            _selectedInstance.update { null }
            kubriko.actorManager.remove(selectedGameObject)
        }
    }

    fun onMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateSelectedInstance() {
        (_selectedInstance.value as? Visible)?.let { visibleTrait ->
            kubriko.viewportManager.setCenter(visibleTrait.position)
        }
    }

    fun notifySelectedInstanceUpdate() = instanceUpdateTrigger.update { !it }

    fun selectInstance(typeId: String) = _selectedTypeId.update { typeId }

    fun deselectSelectedInstance() {
        // TODO _selectedInstance.value?.isSelectedInEditor = false
        _selectedInstance.update { null }
    }

    fun reset() {
        kubriko.viewportManager.setCenter(SceneOffset.Zero)
        _currentFileName.update { DEFAULT_SCENE_FILE_NAME }
        _selectedInstance.update { null }
        kubriko.actorManager.removeAll()
        kubriko.actorManager.add(actors = editorActors)
    }

    fun loadMap(path: String) {
        launch {
            loadFile(path)?.let { json ->
                kubriko.actorManager.deserializeState(json)
                kubriko.actorManager.add(actors = editorActors)
                _currentFileName.update { path.split('/').last() }
            }
        }
    }

    fun saveMap(path: String) {
        launch {
            saveFile(
                path = path,
                content = kubriko.actorManager.serializeState(),
            )
        }
    }

    private fun navigateBack() {
        if (selectedUpdatableInstance.value.first == null) {
            exitApp()
        } else {
            deselectSelectedInstance()
        }
    }

    companion object {
        const val SCENES_DIRECTORY = "./src/commonMain/composeResources/files/scenes"
        private const val DEFAULT_SCENE_FILE_NAME = "scene_untitled.json"
    }
}