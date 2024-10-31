package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.editor.implementation.helpers.exitApp
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeyReleased
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeys
import com.pandulapeter.gameTemplate.editor.implementation.helpers.loadFile
import com.pandulapeter.gameTemplate.editor.implementation.helpers.saveFile
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toWorldCoordinates
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
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

internal class EditorController(val engine: Engine) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val allInstances = engine.instanceManager.allInstances
        .map { it.filterIsInstance<AvailableInEditor<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val visibleInstancesWithinViewport = engine.instanceManager.visibleInstancesWithinViewport
        .map { it.filterIsInstance<AvailableInEditor<*>>() }
        .stateIn(this, SharingStarted.Eagerly, emptyList())
    val totalGameObjectCount = engine.metadataManager.totalGameObjectCount
    private val mouseScreenCoordinates = MutableStateFlow(Offset.Zero)
    val mouseWorldCoordinates = combine(
        mouseScreenCoordinates,
        engine.viewportManager.center,
        engine.viewportManager.size,
        engine.viewportManager.scaleFactor,
    ) { mouseScreenCoordinates, viewportCenter, viewportSize, viewportScaleFactor ->
        mouseScreenCoordinates.toWorldCoordinates(
            viewportCenter = viewportCenter,
            viewportSize = viewportSize,
            viewportScaleFactor = viewportScaleFactor,
        )
    }.stateIn(this, SharingStarted.Eagerly, WorldCoordinates.Zero)
    private val instanceUpdateTrigger = MutableStateFlow(false)
    private val _selectedInstance = MutableStateFlow<AvailableInEditor<*>?>(null)
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
        engine.inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(engine.viewportManager::handleKeys)
            .launchIn(this)
        engine.inputManager.onKeyReleased
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
        val positionInWorld = screenCoordinates.toWorldCoordinates(engine.viewportManager)
        findInstanceOnPosition(positionInWorld).let { gameObjectAtPosition ->
            selectedUpdatableInstance.value.first.let { currentSelectedGameObject ->
                if (gameObjectAtPosition == null) {
                    if (currentSelectedGameObject == null) {
                        launch {
                            val typeId = selectedTypeId.value
                            engine.serializationManager.deserializeInstanceStates(
                                serializedStates = "[{\"typeId\":\"$typeId\",\"state\":\"{\\\"position\\\":{\\\"x\\\":${positionInWorld.x},\\\"y\\\":${positionInWorld.y}}}\"}]"
                            ).firstOrNull()?.restore()?.let { gameObject ->
                                engine.instanceManager.add(gameObject)
                            }
                        }
                    } else {
                        deselectSelectedInstance()
                    }
                } else {
                    (gameObjectAtPosition as? AvailableInEditor<*>)?.let { availableInEditor ->
                        selectInstance(gameObjectAtPosition)
                    }
                }
            }
        }
    }

    fun onRightClick(screenCoordinates: Offset) {
        findInstanceOnPosition(screenCoordinates.toWorldCoordinates(engine.viewportManager)).let { gameObjectAtPosition ->
            if (gameObjectAtPosition != null) {
                if (gameObjectAtPosition == _selectedInstance.value) {
                    deleteSelectedInstance()
                } else {
                    engine.instanceManager.remove(gameObjectAtPosition)
                }
            }
        }
    }

    private fun findInstanceOnPosition(positionInWorld: WorldCoordinates) = engine.instanceManager
        .findVisibleInstancesWithBoundsInPosition(positionInWorld)
        .minByOrNull { (it as? Visible)?.drawingOrder ?: 0f }

    fun selectInstance(gameObject: AvailableInEditor<*>) {
        val currentSelectedGameObject = selectedUpdatableInstance.value.first
        currentSelectedGameObject?.isSelectedInEditor = false
        _selectedInstance.update {
            if (currentSelectedGameObject == gameObject) {
                null
            } else {
                gameObject.also { it.isSelectedInEditor = true }
            }
        }
    }

    fun deleteSelectedInstance() {
        _selectedInstance.value?.let { selectedGameObject ->
            _selectedInstance.update { null }
            engine.instanceManager.remove(selectedGameObject)
        }
    }

    fun onMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateSelectedInstance() {
        (_selectedInstance.value as? Visible)?.let { visibleTrait ->
            engine.viewportManager.setCenter(visibleTrait.position)
        }
    }

    fun notifySelectedInstanceUpdate() = instanceUpdateTrigger.update { !it }

    fun selectInstance(typeId: String) = _selectedTypeId.update { typeId }

    fun deselectSelectedInstance() {
        _selectedInstance.value?.isSelectedInEditor = false
        _selectedInstance.update { null }
    }

    fun reset() {
        _currentFileName.update { DEFAULT_MAP_FILE_NAME }
        _selectedInstance.update { null }
        engine.instanceManager.removeAll()
    }

    fun loadMap(path: String) {
        launch {
            loadFile(path)?.let { json ->
                engine.instanceManager.deserializeState(json)
                _currentFileName.update { path.split('/').last() }
            }
        }
    }

    fun saveMap(path: String) {
        launch {
            saveFile(
                path = path,
                content = engine.instanceManager.serializeState(),
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