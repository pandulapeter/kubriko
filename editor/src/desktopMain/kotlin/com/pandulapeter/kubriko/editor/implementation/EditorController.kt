package com.pandulapeter.kubriko.editor.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.editor.implementation.helpers.exitApp
import com.pandulapeter.kubriko.editor.implementation.helpers.handleKeyReleased
import com.pandulapeter.kubriko.editor.implementation.helpers.handleKeys
import com.pandulapeter.kubriko.editor.implementation.helpers.loadFile
import com.pandulapeter.kubriko.editor.implementation.helpers.saveFile
import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubriko.engine.traits.Editable
import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.implementation.extensions.toWorldCoordinates
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
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
    val allInstances = kubriko.instanceManager.allActors
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val visibleInstancesWithinViewport = kubriko.instanceManager.visibleActorsWithinViewport
        .map { it.filterIsInstance<Editable<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val totalGameObjectCount = kubriko.metadataManager.totalGameObjectCount
    private val mouseScreenCoordinates = MutableStateFlow(Offset.Zero)
    val mouseWorldCoordinates = combine(
        mouseScreenCoordinates,
        kubriko.viewportManager.center,
        kubriko.viewportManager.size,
        kubriko.viewportManager.scaleFactor,
    ) { mouseScreenCoordinates, viewportCenter, viewportSize, viewportScaleFactor ->
        mouseScreenCoordinates.toWorldCoordinates(
            viewportCenter = viewportCenter,
            viewportSize = viewportSize,
            viewportScaleFactor = viewportScaleFactor,
        )
    }.stateIn(this, SharingStarted.Eagerly, WorldCoordinates.Zero)
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
    private val _currentFileName = MutableStateFlow(DEFAULT_MAP_FILE_NAME)
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

    fun getMouseWorldCoordinates() = mouseWorldCoordinates.value

    fun onLeftClick(screenCoordinates: Offset) {
        val positionInWorld = screenCoordinates.toWorldCoordinates(kubriko.viewportManager)
        findInstanceOnPosition(positionInWorld).let { gameObjectAtPosition ->
            selectedUpdatableInstance.value.first.let { currentSelectedGameObject ->
                if (gameObjectAtPosition == null) {
                    if (currentSelectedGameObject == null) {
                        launch {
                            val typeId = selectedTypeId.value
                            kubriko.serializationManager.deserializeActors(
                                serializedStates = "[{\"typeId\":\"$typeId\",\"state\":\"{\\\"position\\\":{\\\"x\\\":${positionInWorld.x},\\\"y\\\":${positionInWorld.y}}}\"}]"
                            ).firstOrNull()?.let { actor ->
                                kubriko.instanceManager.add(actor)
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
        findInstanceOnPosition(screenCoordinates.toWorldCoordinates(kubriko.viewportManager)).let { actorAtPosition ->
            if (actorAtPosition != null) {
                if (actorAtPosition == _selectedInstance.value) {
                    deleteSelectedInstance()
                } else {
                    kubriko.instanceManager.remove(actorAtPosition)
                }
            }
        }
    }

    private fun findInstanceOnPosition(positionInWorld: WorldCoordinates) = kubriko.instanceManager
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
            kubriko.instanceManager.remove(selectedGameObject)
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
        kubriko.viewportManager.setCenter(WorldCoordinates.Zero)
        _currentFileName.update { DEFAULT_MAP_FILE_NAME }
        _selectedInstance.update { null }
        kubriko.instanceManager.removeAll()
    }

    fun loadMap(path: String) {
        launch {
            loadFile(path)?.let { json ->
                kubriko.instanceManager.deserializeState(json)
                _currentFileName.update { path.split('/').last() }
            }
        }
    }

    fun saveMap(path: String) {
        launch {
            saveFile(
                path = path,
                content = kubriko.instanceManager.serializeState(),
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
        const val MAPS_DIRECTORY = "./src/commonMain/composeResources/files/maps"
        private const val DEFAULT_MAP_FILE_NAME = "map_untitled.json"
    }
}