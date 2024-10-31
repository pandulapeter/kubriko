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

internal class InstanceManagerImpl(
    private val engineImpl: EngineImpl,
) : InstanceManager {

    private val _allInstances = MutableStateFlow(emptyList<Any>())
    override val allInstances = _allInstances.asStateFlow()
    val dynamicInstances = _allInstances
        .map { gameObjects -> gameObjects.filterIsInstance<Dynamic>() }
        .stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    private val visibleInstances = _allInstances
        .map { gameObjects -> gameObjects.filterIsInstance<Visible>() }
        .stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    override val visibleInstancesWithinViewport = combine(
        engineImpl.metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
        visibleInstances,
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

    override fun add(vararg instances: Any) = _allInstances.update { currentValue ->
        val uniqueGameObjects = instances.filterIsInstance<Unique>()
        if (uniqueGameObjects.isEmpty()) {
            currentValue
        } else {
            val filteredCurrentValue = currentValue.toMutableList()
            uniqueGameObjects.forEach { unique ->
                filteredCurrentValue.removeAll { it::class == unique::class }
            }
            filteredCurrentValue
        } + instances
    }

    override suspend fun serializeState() =
        engineImpl.serializationManager.serializeInstanceStates(allInstances.value.filterIsInstance<AvailableInEditor<*>>().map { it.saveState() })

    override suspend fun deserializeState(json: String) {
        removeAll()
        add(instances = engineImpl.serializationManager.deserializeInstanceStates(json).mapNotNull { it.restore() }.toTypedArray())
    }

    override fun remove(vararg instances: Any) = _allInstances.update { currentValue ->
        currentValue.filterNot { it in instances }
    }

    override fun removeAll() = _allInstances.update { emptyList() }

    override fun findVisibleInstancesWithBoundsInPosition(position: WorldCoordinates) = visibleInstancesWithinViewport.value
        .filter { it.occupiesPosition(position) }

    override fun findVisibleInstancesWithPivotsAroundPosition(position: WorldCoordinates, range: Float) = visibleInstances.value
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
}