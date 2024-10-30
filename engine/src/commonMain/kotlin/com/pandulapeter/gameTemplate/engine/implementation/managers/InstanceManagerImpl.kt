package com.pandulapeter.gameTemplate.engine.implementation.managers

import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Unique
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isAroundPosition
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isVisible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.gameTemplate.engine.managers.InstanceManager
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

internal class InstanceManagerImpl(
    private val engineImpl: EngineImpl,
    vararg typesAvailableInEditor: Triple<String, KClass<*>, (String) -> AvailableInEditor.State<*>>
) : InstanceManager {

    val typeIdsForEditorRegistry = typesAvailableInEditor.associate { (typeId, _, deserializer) -> typeId to deserializer }
    private val typeResolvers = typesAvailableInEditor.associate { (typeId, type, _) -> type to typeId }
    override val typeIdsForEditor = typeIdsForEditorRegistry.keys
    private val _gameObjects = MutableStateFlow(emptyList<Any>())
    override val allInstances = _gameObjects.asStateFlow()
    val dynamicGameObjects = _gameObjects.map { gameObjects -> gameObjects.filterIsInstance<Dynamic>() }.stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    private val visibleGameObjects = _gameObjects.map { gameObjects -> gameObjects.filterIsInstance<Visible>() }.stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    override val visibleInstancesWithinViewport = combine(
        engineImpl.metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
        visibleGameObjects,
        engineImpl.viewportManager.size,
        engineImpl.viewportManager.center,
        engineImpl.viewportManager.scaleFactor,
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
    }.stateIn(engineImpl, SharingStarted.Eagerly, emptyList())

    override fun resolveTypeId(type: KClass<*>) = typeResolvers[type].orEmpty()

    override fun add(vararg gameObjects: Any) = _gameObjects.update { currentValue ->
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

    override suspend fun serializeState() =
        engineImpl.serializationManager.serializeGameObjectStates(allInstances.value.filterIsInstance<AvailableInEditor<*>>().map { it.saveState() })

    override suspend fun deserializeState(json: String) {
        removeAll()
        add(gameObjects = engineImpl.serializationManager.deserializeGameObjectStates(json).mapNotNull { it.restore() }.toTypedArray())
    }

    override fun remove(vararg gameObjects: Any) = _gameObjects.update { currentValue ->
        currentValue.filterNot { it in gameObjects }
    }

    override fun removeAll() = _gameObjects.update { emptyList() }

    override fun findGameObjectsWithBoundsInPosition(position: WorldCoordinates) = visibleInstancesWithinViewport.value
        .filter { it.occupiesPosition(position) }

    override fun findGameObjectsWithPivotsAroundPosition(position: WorldCoordinates, range: Float) = visibleGameObjects.value
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
}