package com.pandulapeter.kubriko.engine.implementation.managers

import com.pandulapeter.kubriko.engine.actor.Actor
import com.pandulapeter.kubriko.engine.actor.traits.Editable
import com.pandulapeter.kubriko.engine.actor.traits.Dynamic
import com.pandulapeter.kubriko.engine.actor.traits.Unique
import com.pandulapeter.kubriko.engine.actor.traits.Visible
import com.pandulapeter.kubriko.engine.implementation.KubrikoImpl
import com.pandulapeter.kubriko.engine.implementation.extensions.isAroundPosition
import com.pandulapeter.kubriko.engine.implementation.extensions.isVisible
import com.pandulapeter.kubriko.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.kubriko.engine.managers.InstanceManager
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class InstanceManagerImpl(
    private val engineImpl: KubrikoImpl,
) : InstanceManager {

    private val _allInstances = MutableStateFlow(emptyList<Actor>())
    override val allActors = _allInstances.asStateFlow()
    val dynamicInstances = _allInstances
        .map { gameObjects -> gameObjects.filterIsInstance<Dynamic>() }
        .stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    private val visibleInstances = _allInstances
        .map { gameObjects -> gameObjects.filterIsInstance<Visible>() }
        .stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    override val visibleActorsWithinViewport = combine(
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

    @OptIn(ExperimentalUuidApi::class)
    override fun add(vararg actors: Actor) = _allInstances.update { currentValue ->
        val uniqueGameObjects = actors.filterIsInstance<Unique>()
        if (uniqueGameObjects.isEmpty()) {
            currentValue
        } else {
            val filteredCurrentValue = currentValue.toMutableList()
            uniqueGameObjects.forEach { unique ->
                filteredCurrentValue.removeAll { it::class == unique::class }
            }
            filteredCurrentValue
        } + actors.onEach { actor ->
            actor.instanceId = Uuid.random().toString()
        }
    }

    override fun remove(vararg actors: Actor) = _allInstances.update { currentValue ->
        currentValue.filterNot { it in actors }
    }

    override fun removeAll() = _allInstances.update { emptyList() }

    override suspend fun serializeState() =
        engineImpl.serializationManager.serializeInstanceStates(allActors.value.filterIsInstance<Editable<*>>().map { it.saveState() })

    override suspend fun deserializeState(json: String) {
        removeAll()
        // TODO: Weird things happen at this point once we try to restore more than 20000 Actors. Singletons constructors get invoked again.
        add(actors = engineImpl.serializationManager.deserializeInstanceStates(json).map { it.restore() }.toTypedArray())
    }

    override fun findVisibleInstancesWithBoundsInPosition(position: WorldCoordinates) = visibleActorsWithinViewport.value
        .filter { it.occupiesPosition(position) }
        .map { it as Actor }

    override fun findVisibleInstancesWithPivotsAroundPosition(position: WorldCoordinates, range: Float) = visibleInstances.value
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
        .map { it as Actor }
}