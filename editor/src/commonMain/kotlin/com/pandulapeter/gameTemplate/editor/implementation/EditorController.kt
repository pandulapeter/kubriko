package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.editor.GameObjects
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeyReleased
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeys
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toPositionInWorld
import com.pandulapeter.gameTemplate.gameplayObjects.DynamicBox
import com.pandulapeter.gameTemplate.gameplayObjects.Marker
import com.pandulapeter.gameTemplate.gameplayObjects.StaticBox
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
import java.io.File

internal object EditorController : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val totalGameObjectCount = Engine.get().metadataManager.totalGameObjectCount
    private val mouseScreenCoordinates = MutableStateFlow(Offset.Zero)
    val mouseWorldPosition = combine(
        mouseScreenCoordinates,
        Engine.get().viewportManager.offset,
        Engine.get().viewportManager.size,
        Engine.get().viewportManager.scaleFactor,
    ) { mouseScreenCoordinates, _, _, _ ->
        mouseScreenCoordinates.toPositionInWorld()
    }.stateIn(this, SharingStarted.Eagerly, Offset.Zero)
    private val triggerGameObjectUpdate = MutableStateFlow(false)
    private val _selectedGameObject = MutableStateFlow<GameObject?>(null)
    val selectedGameObject = combine(
        _selectedGameObject,
        triggerGameObjectUpdate,
    ) { selectedGameObject, triggerGameObjectUpdate ->
        selectedGameObject to triggerGameObjectUpdate
    }.stateIn(this, SharingStarted.Eagerly, null to false)
    private val _selectedGameObjectType = MutableStateFlow<Class<out GameObject>>(StaticBox::class.java)
    val selectedGameObjectType = _selectedGameObjectType.asStateFlow()

    init {
        Engine.get().inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(::handleKeys)
            .launchIn(this)
        Engine.get().inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
    }

    fun handleClick(screenCoordinates: Offset) = Engine.get().gameObjectManager.run {
        val positionInWorld = screenCoordinates.toPositionInWorld()
        val gameObjectAtPosition = findGameObjectsWithBoundsInPosition(positionInWorld).minByOrNull { it.depth } as? GameObject
        selectedGameObject.value.first.let { currentSelectedGameObject ->
            if (gameObjectAtPosition == null) {
                if (currentSelectedGameObject == null) {
                    GameObjects.supportedGameObjectTypes[selectedGameObjectType.value]?.invoke(positionInWorld)?.let { add(it) }
                } else {
                    currentSelectedGameObject.isSelectedInEditor = false
                    _selectedGameObject.update { null }
                }
            } else {
                currentSelectedGameObject?.isSelectedInEditor = false
                _selectedGameObject.update {
                    if (currentSelectedGameObject == gameObjectAtPosition) {
                        null
                    } else {
                        gameObjectAtPosition.also { it.isSelectedInEditor = true }
                    }
                }
            }
        }
    }

    fun handleMouseMove(screenCoordinates: Offset) = mouseScreenCoordinates.update { screenCoordinates }

    fun locateGameObject() {
        (_selectedGameObject.value as? Visible)?.let { selectedGameObject ->
            Engine.get().viewportManager.setOffset(selectedGameObject.position)
        }
    }

    fun deleteGameObject() {
        _selectedGameObject.value?.let { selectedGameObject ->
            _selectedGameObject.update { null }
            Engine.get().gameObjectManager.remove(selectedGameObject)
        }
    }

    fun notifyGameObjectUpdate() = triggerGameObjectUpdate.update { !it }

    fun selectGameObjectType(gameObjectType: Class<out GameObject>) = _selectedGameObjectType.update { gameObjectType }

    fun reset() {
        _selectedGameObject.update { null }
        Engine.get().gameObjectManager.removeAll()
    }

    fun getExistingMapNames() = File("../${Engine.MAPS_LOCATION}").let { currentDirectory ->
        if (!currentDirectory.exists()) {
            currentDirectory.mkdir()
        }
        currentDirectory.listFiles().orEmpty().map { it.nameWithoutExtension }
    }
}