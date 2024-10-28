package com.pandulapeter.gameTemplate.engine.implementation.managers

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isAroundPosition
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isVisible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class GameObjectManagerImpl : GameObjectManager {

    private val _gameObjectStateRegistry = MutableStateFlow(mapOf<String, (String) -> GameObject.State<*>>())
    val gameObjectStateRegistry = _gameObjectStateRegistry.asStateFlow()
    override val registeredTypeIds = _gameObjectStateRegistry.map { it.keys.toList() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    private val _gameObjects = MutableStateFlow(emptyList<GameObject<*>>())
    val gameObjects = _gameObjects.asStateFlow()
    val dynamicGameObjects = _gameObjects.map { it.filterIsInstance<Dynamic>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    private val visibleGameObjects = _gameObjects.map { it.filterIsInstance<Visible>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
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


    override fun register(vararg entries: Pair<String, (String) -> GameObject.State<*>>) = _gameObjectStateRegistry.update { currentValue ->
        currentValue.toMutableMap().also { mutableMap ->
            entries.forEach { (typeId, deserializer) ->
                mutableMap[typeId] = deserializer
            }
        }.toMap()
    }

    override fun add(vararg gameObjects: GameObject<*>) = _gameObjects.update { currentValue ->
        currentValue + gameObjects
    }

    override suspend fun serializeState() = EngineImpl.serializationManager.serializeGameObjectStates(gameObjects.value.map { it.getState() })

    override suspend fun deserializeState(json: String) {
        removeAll()
        add(gameObjects = EngineImpl.serializationManager.deserializeGameObjectStates(json).map { it.instantiate() }.toTypedArray())
    }

    override fun remove(vararg gameObjects: GameObject<*>) = _gameObjects.update { currentValue ->
        currentValue.filterNot { it in gameObjects }
    }

    override fun removeAll() = _gameObjects.update { emptyList() }

    override fun findGameObjectsWithBoundsInPosition(position: Offset) = visibleGameObjectsInViewport.value
        .filter { it.occupiesPosition(position) }

    override fun findGameObjectsWithPivotsAroundPosition(position: Offset, range: Float) = visibleGameObjects.value
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
}