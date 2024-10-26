package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeyReleased
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeys
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toPositionInWorld
import com.pandulapeter.gameTemplate.gameplayObjects.DynamicBox
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

internal object EditorController : CoroutineScope {

    val supportedGameObjectTypes = mapOf<Class<out GameObject>, (Offset) -> GameObject>(
        StaticBox::class.java to {
            StaticBox(
                color = Color.Red,
                edgeSize = 100f,
                position = it,
                rotationDegrees = 0f,
            )
        },
        DynamicBox::class.java to {
            DynamicBox(
                color = Color.Red,
                edgeSize = 100f,
                position = it,
                rotationDegrees = 0f,
                scaleFactor = 1f,
            )
        }
    )

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
        val gameObjectAtPosition = findGameObjectsWithBoundsInPosition(screenCoordinates.toPositionInWorld()).minByOrNull { it.depth } as? GameObject
        selectedGameObject.value.first.let { currentSelectedGameObject ->
            if (gameObjectAtPosition == null) {
                if (currentSelectedGameObject == null) {
                    supportedGameObjectTypes[selectedGameObjectType.value]?.invoke(screenCoordinates.toPositionInWorld())?.let { add(it) }
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
}