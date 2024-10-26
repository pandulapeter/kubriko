package com.pandulapeter.gameTemplate.engine.implementation.managers

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isAroundPosition
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isVisible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toWorldCoordinates
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class GameObjectManagerImpl : GameObjectManager {

    val gameObjects = MutableStateFlow(emptyList<GameObject>())
    val dynamicGameObjects = gameObjects.map { it.filterIsInstance<Dynamic>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    val visibleGameObjects = gameObjects.map { it.filterIsInstance<Visible>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    val visibleGameObjectsInViewport = combine(
        EngineImpl.metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
        visibleGameObjects,
        EngineImpl.viewportManager.size,
        EngineImpl.viewportManager.offset,
        EngineImpl.viewportManager.scaleFactor,
    ) { _, allVisibleGameObjects, viewportSize, viewportOffset, viewportScaleFactor ->
        allVisibleGameObjects
            .filter {
                it.isVisible(
                    scaledHalfViewportSize = viewportSize / (viewportScaleFactor * 2),
                    viewportOffset = viewportOffset,
                    viewportScaleFactor = viewportScaleFactor,
                )
            }
            .sortedByDescending { it.depth }
    }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())

    override fun register(gameObject: GameObject) = gameObjects.update { currentValue ->
        currentValue + gameObject
    }

    override fun register(gameObjects: Collection<GameObject>) = EngineImpl.gameObjectManager.gameObjects.update { currentValue ->
        currentValue + gameObjects
    }

    override fun remove(gameObject: GameObject) = gameObjects.update { currentValue ->
        currentValue.filterNot { it == gameObject }
    }

    override fun remove(gameObjects: Collection<GameObject>) = EngineImpl.gameObjectManager.gameObjects.update { currentValue ->
        currentValue.filterNot { it in gameObjects }
    }

    override fun removeAll() = gameObjects.update { emptyList() }

    override fun findGameObjectsOnScreenCoordinates(screenCoordinates: Offset) = screenCoordinates.toWorldCoordinates(
        viewportOffset = EngineImpl.viewportManager.offset.value,
        scaledHalfViewportSize = EngineImpl.viewportManager.size.value / 2f,
        viewportScaleFactor = EngineImpl.viewportManager.scaleFactor.value,
    ).let { worldCoordinates ->
        visibleGameObjectsInViewport.value
            .filter { it.occupiesPosition(worldCoordinates) }
    }

    override fun findGameObjectsAroundPosition(position: Offset, range: Float) = visibleGameObjects.value
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
}