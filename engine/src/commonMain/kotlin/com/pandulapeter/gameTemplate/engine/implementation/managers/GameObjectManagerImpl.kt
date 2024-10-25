package com.pandulapeter.gameTemplate.engine.implementation.managers

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isVisible
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class GameObjectManagerImpl : GameObjectManager {

    val gameObjects = MutableStateFlow(emptySet<GameObject>())
    val dynamicGameObjects = gameObjects.map { it.filterIsInstance<Dynamic>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    val visibleGameObjectsInViewport = combine(
        EngineImpl.metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
        gameObjects.map { it.filterIsInstance<Visible>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList()),
        EngineImpl.viewportManager.size,
        EngineImpl.viewportManager.offset,
        EngineImpl.viewportManager.scaleFactor,
    ) { _, allVisibleGameObjects, viewportSize, viewportOffset, viewportScaleFactor ->
        (viewportSize / viewportScaleFactor).let { scaledViewportSize ->
            allVisibleGameObjects.filter {
                it.isVisible(
                    scaledViewportSize = scaledViewportSize,
                    viewportOffset = viewportOffset,
                    viewportScaleFactor = viewportScaleFactor,
                )
            }
        }
    }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())

    override fun register(gameObject: GameObject) = gameObjects.update { currentValue ->
        currentValue + gameObject
    }

    override fun register(gameObjects: Collection<GameObject>) = EngineImpl.gameObjectManager.gameObjects.update { currentValue ->
        currentValue + gameObjects
    }

    override fun remove(gameObject: GameObject) = gameObjects.update { currentValue ->
        currentValue.filterNot { it == gameObject }.toSet()
    }

    override fun remove(gameObjects: Collection<GameObject>) = EngineImpl.gameObjectManager.gameObjects.update { currentValue ->
        currentValue.filterNot { it in gameObjects }.toSet()
    }

    override fun removeAll() = gameObjects.update { emptySet() }
}