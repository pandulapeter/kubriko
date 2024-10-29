package com.pandulapeter.gameTemplate.engine.implementation.managers

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Unique
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.getTrait
import com.pandulapeter.gameTemplate.engine.implementation.extensions.hasTrait
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isAroundPosition
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isVisible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import com.pandulapeter.gameTemplate.engine.types.MapCoordinates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class GameObjectManagerImpl : GameObjectManager {

    private val _gameObjectSerializerRegistry = MutableStateFlow(mapOf<String, (String) -> Serializer<*>>())
    val gameObjectStateRegistry = _gameObjectSerializerRegistry.asStateFlow()
    override val registeredTypeIdsForEditor = _gameObjectSerializerRegistry
        // TODO: Filter for AvailableInEditor trait
        .map { it.keys.toList() }
        .stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    private val _gameObjects = MutableStateFlow(emptyList<GameObject<*>>())
    val gameObjects = _gameObjects.asStateFlow()
    val dynamicTraits = _gameObjects.map { gameObjects -> gameObjects.mapNotNull { it.getTrait<Dynamic>() } }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    private val visibleGameObjects = _gameObjects.map { gameObjects -> gameObjects.filter { it.hasTrait<Visible>() } }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    val visibleGameObjectsInViewport = combine(
        EngineImpl.metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
        visibleGameObjects,
        EngineImpl.viewportManager.size,
        EngineImpl.viewportManager.center,
        EngineImpl.viewportManager.scaleFactor,
    ) { _, allVisibleGameObjects, viewportSize, viewportCenter, viewportScaleFactor ->
        allVisibleGameObjects
            .filter {
                it.getTrait<Visible>()?.isVisible(
                    scaledHalfViewportSize = viewportSize / (viewportScaleFactor * 2),
                    viewportCenter = viewportCenter,
                    viewportScaleFactor = viewportScaleFactor,
                ) == true
            }
            .sortedByDescending { it.getTrait<Visible>()?.depth }
    }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())


    override fun register(vararg entries: Pair<String, (String) -> Serializer<*>>) = _gameObjectSerializerRegistry.update { currentValue ->
        currentValue.toMutableMap().also { mutableMap ->
            entries.forEach { (typeId, deserializer) ->
                mutableMap[typeId] = deserializer
            }
        }.toMap()
    }

    override fun add(vararg gameObjects: GameObject<*>) = _gameObjects.update { currentValue ->
        val uniqueGameObjects = gameObjects.filter { it.hasTrait<Unique>() }
        if (uniqueGameObjects.isEmpty()) {
            currentValue
        } else {
            val filteredCurrentValue = currentValue.toMutableList()
            uniqueGameObjects.forEach { unique ->
                filteredCurrentValue.removeAll { it::class == unique::class }
            }
            filteredCurrentValue
        } + gameObjects
    }

    override suspend fun serializeState() = EngineImpl.serializationManager.serializeGameObjectStates(gameObjects.value.map { it.getSerializer() })

    override suspend fun deserializeState(json: String) {
        removeAll()
        add(gameObjects = EngineImpl.serializationManager.deserializeGameObjectStates(json).map { it.instantiate() }.filterIsInstance<GameObject<*>>().toTypedArray())
    }

    override fun remove(vararg gameObjects: GameObject<*>) = _gameObjects.update { currentValue ->
        currentValue.filterNot { it in gameObjects }
    }

    override fun removeAll() = _gameObjects.update { emptyList() }

    override fun findGameObjectsWithBoundsInPosition(position: MapCoordinates) = visibleGameObjectsInViewport.value
        .filter { it.getTrait<Visible>()?.occupiesPosition(position) == true }

    override fun findGameObjectsWithPivotsAroundPosition(position: MapCoordinates, range: Float) = visibleGameObjects.value
        .filter {
            it.getTrait<Visible>()?.isAroundPosition(
                position = position,
                range = range,
            ) == true
        }
}