package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeyReleased
import com.pandulapeter.gameTemplate.editor.implementation.helpers.handleKeys
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toPositionInWorld
import com.pandulapeter.gameTemplate.gameplayObjects.StaticBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal object EditorController : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val totalGameObjectCount = Engine.get().metadataManager.totalGameObjectCount
    private val _mouseWorldPosition = MutableStateFlow(Offset.Zero)
    val mouseWorldPosition = _mouseWorldPosition.asStateFlow()
    private val _selectedGameObject = MutableStateFlow<GameObject?>(null)
    val selectedGameObject = _selectedGameObject.asStateFlow()

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
        if (gameObjectAtPosition == null) {
            register(
                StaticBox(
                    color = Color.Red,
                    edgeSize = 100f,
                    position = screenCoordinates.toPositionInWorld(),
                    rotationDegrees = 0f,
                )
            )
        } else {
            _selectedGameObject.update { gameObjectAtPosition }
        }
    }

    fun handleMouseMove(screenCoordinates: Offset) = _mouseWorldPosition.update { screenCoordinates.toPositionInWorld() }

    fun unselectGameObject() = _selectedGameObject.update { null }

    fun updateSelectedGameObject(updatedGameObject: GameObject) {
        _selectedGameObject.value?.let { selectedGameObject ->
            Engine.get().gameObjectManager.remove(selectedGameObject)
            Engine.get().gameObjectManager.register(updatedGameObject)
            _selectedGameObject.update { updatedGameObject }
        }
    }

    fun deleteGameObject() {
        _selectedGameObject.value?.let { selectedGameObject ->
            unselectGameObject()
            Engine.get().gameObjectManager.remove(selectedGameObject)
        }
    }
}