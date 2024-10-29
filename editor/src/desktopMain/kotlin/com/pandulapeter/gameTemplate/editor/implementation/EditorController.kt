package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.editor.implementation.helpers.exitApp
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeyReleased
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeys
import com.pandulapeter.gameTemplate.editor.implementation.helpers.loadFile
import com.pandulapeter.gameTemplate.editor.implementation.helpers.saveFile
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toMapCoordinates
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object EditorController : CoroutineScope {

    const val MAPS_DIRECTORY = "./src/commonMain/composeResources/files/maps"
    private const val DEFAULT_MAP_FILE_NAME = "map_untitled.json"
    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val totalGameObjectCount = Engine.get().metadataManager.totalGameObjectCount
    private val mouseScreenCoordinates = MutableStateFlow(Offset.Zero)
    val mouseWorldCoordinates = combine(
        mouseScreenCoordinates,
        Engine.get().viewportManager.center,
        Engine.get().viewportManager.size,
        Engine.get().viewportManager.scaleFactor,
    ) { mouseScreenCoordinates, _, _, _ ->
        mouseScreenCoordinates.toMapCoordinates()
    }.stateIn(this, SharingStarted.Eagerly, WorldCoordinates.Zero)
    private val triggerGameObjectUpdate = MutableStateFlow(false)
    private val _selectedGameObject = MutableStateFlow<GameObject<*>?>(null)
    val selectedGameObject = combine(
        _selectedGameObject,
        triggerGameObjectUpdate,
    ) { selectedGameObject, triggerGameObjectUpdate ->
        selectedGameObject to triggerGameObjectUpdate
    }.stateIn(this, SharingStarted.Eagerly, null to false)
    private val _selectedGameObjectType = MutableStateFlow<String?>(null)
    val selectedGameObjectTypeId = _selectedGameObjectType.asStateFlow()
    private val _currentFileName = MutableStateFlow(DEFAULT_MAP_FILE_NAME)
    val currentFileName = _currentFileName.asStateFlow()
    private val _expandedCategories = MutableStateFlow(emptySet<String>())
    val expandedCategories = _expandedCategories.asStateFlow()

    init {
        Engine.get().inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(::handleKeys)
            .launchIn(this)
        Engine.get().inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        _selectedGameObject
            .onEach { _expandedCategories.update { emptySet() } }
            .launchIn(this)
    }

    fun handleLeftClick(screenCoordinates: Offset) {
        val positionInWorld = screenCoordinates.toMapCoordinates()
        findGameObjectOnPosition(positionInWorld).let { gameObjectAtPosition ->
            selectedGameObject.value.first.let { currentSelectedGameObject ->
                if (gameObjectAtPosition == null) {
                    if (currentSelectedGameObject == null) {
                        launch {
                            val typeId = selectedGameObjectTypeId.value
                            // TODO: Use AvailableInEditor trait instead
                            Engine.get().serializationManager.deserializeGameObjectStates(
                                serializedStates = "[{\"typeId\":\"$typeId\",\"state\":\"{\\\"position\\\":{\\\"x\\\":${positionInWorld.x},\\\"y\\\":${positionInWorld.y}}}\"}]"
                            ).firstOrNull()?.restore()?.let { gameObject ->
                                Engine.get().gameObjectManager.add(gameObject as GameObject<*>)
                            }
                        }
                    } else {
                        unselectGameObject()
                    }
                } else {
                    (gameObjectAtPosition as? AvailableInEditor)?.let { availableInEditor ->
                        (currentSelectedGameObject as? AvailableInEditor)?.isSelectedInEditor = false
                        _selectedGameObject.update {
                            if (currentSelectedGameObject == gameObjectAtPosition) {
                                null
                            } else {
                                gameObjectAtPosition.also { availableInEditor.isSelectedInEditor = true }
                            }
                        }
                    }
                }
            }
        }
    }

    fun handleRightClick(screenCoordinates: Offset) {
        findGameObjectOnPosition(screenCoordinates.toMapCoordinates()).let { gameObjectAtPosition ->
            if (gameObjectAtPosition != null) {
                if (gameObjectAtPosition == _selectedGameObject.value) {
                    deleteSelectedGameObject()
                } else {
                    Engine.get().gameObjectManager.remove(gameObjectAtPosition)
                }
            }
        }
    }

    private fun findGameObjectOnPosition(positionInWorld: WorldCoordinates) = Engine.get().gameObjectManager
        .findGameObjectsWithBoundsInPosition(positionInWorld)
        .minByOrNull { (it as? Visible)?.drawingOrder ?: 0f }

    fun unselectGameObject() {
        (_selectedGameObject.value as? AvailableInEditor)?.isSelectedInEditor = false
        _selectedGameObject.update { null }
    }

    fun handleMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateGameObject() {
        (_selectedGameObject.value as? Visible)?.let { visibleTrait ->
            Engine.get().viewportManager.setCenter(visibleTrait.position)
        }
    }

    fun deleteSelectedGameObject() {
        _selectedGameObject.value?.let { selectedGameObject ->
            _selectedGameObject.update { null }
            Engine.get().gameObjectManager.remove(selectedGameObject)
        }
    }

    fun notifyGameObjectUpdate() = triggerGameObjectUpdate.update { !it }

    fun selectGameObjectType(gameObjectTypeId: String) = _selectedGameObjectType.update { gameObjectTypeId }

    fun reset() {
        _currentFileName.update { DEFAULT_MAP_FILE_NAME }
        _selectedGameObject.update { null }
        Engine.get().gameObjectManager.removeAll()
    }

    fun loadMap(path: String) {
        launch {
            loadFile(path)?.let { json ->
                Engine.get().gameObjectManager.deserializeState(json)
                _currentFileName.update { path.split('/').last() }
            }
        }
    }

    fun saveMap(path: String) {
        launch {
            saveFile(
                path = path,
                content = Engine.get().gameObjectManager.serializeState(),
            )
        }
    }

    fun navigateBack() {
        if (selectedGameObject.value.first == null) {
            exitApp()
        } else {
            unselectGameObject()
        }
    }
}