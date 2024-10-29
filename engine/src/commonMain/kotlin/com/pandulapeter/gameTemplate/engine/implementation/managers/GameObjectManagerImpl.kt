package com.pandulapeter.gameTemplate.engine.implementation.managers

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.State
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Unique
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isAroundPosition
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isVisible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

internal class GameObjectManagerImpl : GameObjectManager {

    private val _gameObjectStateRegistry = MutableStateFlow(mapOf<String, (String) -> State<*>>())
    val gameObjectStateRegistry = _gameObjectStateRegistry.asStateFlow()
    private var gameObjectTypeIds = emptyMap<KClass<out GameObject<*>>, String>()
    override val registeredTypeIdsForEditor = _gameObjectStateRegistry
        .map { it.keys.toList() }
        .stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    private val _gameObjects = MutableStateFlow(emptyList<GameObject<*>>())
    val gameObjects = _gameObjects.asStateFlow()
    val dynamicGameObjects = _gameObjects.map { gameObjects -> gameObjects.filterIsInstance<Dynamic>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    private val visibleGameObjects = _gameObjects.map { gameObjects -> gameObjects.filterIsInstance<Visible>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    val visibleGameObjectsInViewport = combine(
        EngineImpl.metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
        visibleGameObjects,
        EngineImpl.viewportManager.size,
        EngineImpl.viewportManager.center,
        EngineImpl.viewportManager.scaleFactor,
    ) { _, allVisibleGameObjects, viewportSize, viewportCenter, viewportScaleFactor ->
        allVisibleGameObjects
            .filter {
                it.isVisible(
                    scaledHalfViewportSize = viewportSize / (viewportScaleFactor * 2),
                    viewportCenter = viewportCenter,
                    viewportScaleFactor = viewportScaleFactor,
                )
            }
            .sortedByDescending { it.drawingOrder }
    }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())

    override fun getTypeId(type: KClass<out GameObject<*>>) = gameObjectTypeIds[type].orEmpty()

    override fun register(vararg entries: Triple<String, KClass<out GameObject<*>>, (String) -> State<*>>) = _gameObjectStateRegistry.update { currentValue ->
        gameObjectTypeIds = entries.associate { (typeId, type, _) -> type to typeId }
        currentValue.toMutableMap().also { mutableMap ->
            entries.forEach { (typeId, _, deserializer) ->
                mutableMap[typeId] = deserializer
            }
        }.toMap()
    }

    override fun add(vararg gameObjects: GameObject<*>) = _gameObjects.update { currentValue ->
        val uniqueGameObjects = gameObjects.filterIsInstance<Unique>()
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

    override suspend fun serializeState() = EngineImpl.serializationManager.serializeGameObjectStates(gameObjects.value.map { it.saveState() })

    override suspend fun deserializeState(json: String) {
        removeAll()
        add(gameObjects = EngineImpl.serializationManager.deserializeGameObjectStates(json).map { it.restore() }.filterIsInstance<GameObject<*>>().toTypedArray())
    }

    override fun remove(vararg gameObjects: GameObject<*>) = _gameObjects.update { currentValue ->
        currentValue.filterNot { it in gameObjects }
    }

    override fun removeAll() = _gameObjects.update { emptyList() }

    override fun findGameObjectsWithBoundsInPosition(position: WorldCoordinates) = visibleGameObjectsInViewport.value
        .filter { it.occupiesPosition(position) }
        .map { it as GameObject<*> }

    override fun findGameObjectsWithPivotsAroundPosition(position: WorldCoordinates, range: Float) = visibleGameObjects.value
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
        .map { it as GameObject<*> }
}