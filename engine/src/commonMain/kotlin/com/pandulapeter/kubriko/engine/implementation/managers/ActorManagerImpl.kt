package com.pandulapeter.kubriko.engine.implementation.managers

import com.pandulapeter.kubriko.engine.implementation.KubrikoImpl
import com.pandulapeter.kubriko.engine.implementation.extensions.isAroundPosition
import com.pandulapeter.kubriko.engine.implementation.extensions.isVisible
import com.pandulapeter.kubriko.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.kubriko.engine.managers.ActorManager
import com.pandulapeter.kubriko.engine.traits.Dynamic
import com.pandulapeter.kubriko.engine.traits.Editable
import com.pandulapeter.kubriko.engine.traits.Unique
import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.types.SceneOffset
import com.pandulapeter.kubriko.engine.types.SceneSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class ActorManagerImpl(
    private val engineImpl: KubrikoImpl,
) : ActorManager {

    private val _allActors = MutableStateFlow(emptyList<Any>())
    override val allActors = _allActors.asStateFlow()
    val dynamicActors = _allActors
        .map { gameObjects -> gameObjects.filterIsInstance<Dynamic>() }
        .stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    private val visibleActors = _allActors
        .map { gameObjects -> gameObjects.filterIsInstance<Visible>() }
        .stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    override val visibleActorsWithinViewport = combine(
        engineImpl.metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
        visibleActors,
        engineImpl.viewportManager.size,
        engineImpl.viewportManager.center,
        engineImpl.viewportManager.scaleFactor,
    ) { _, allVisibleGameObjects, viewportSize, viewportCenter, viewportScaleFactor ->
        allVisibleGameObjects
            .filter {
                it.isVisible(
                    scaledHalfViewportSize = SceneSize(viewportSize / (viewportScaleFactor * 2)),
                    viewportCenter = viewportCenter,
                    viewportScaleFactor = viewportScaleFactor,
                )
            }
            .sortedByDescending { it.drawingOrder }
    }.stateIn(engineImpl, SharingStarted.Eagerly, emptyList())

    override fun add(vararg actors: Any) = _allActors.update { currentValue ->
        val uniqueGameObjects = actors.filterIsInstance<Unique>()
        if (uniqueGameObjects.isEmpty()) {
            currentValue
        } else {
            val filteredCurrentValue = currentValue.toMutableList()
            uniqueGameObjects.forEach { unique ->
                filteredCurrentValue.removeAll { it::class == unique::class }
            }
            filteredCurrentValue
        } + actors
    }

    override fun remove(vararg actors: Any) = _allActors.update { currentValue ->
        currentValue.filterNot { it in actors }
    }

    override fun removeAll() = _allActors.update { emptyList() }

    override suspend fun serializeState() =
        engineImpl.serializationManager.serializeActors(allActors.value.filterIsInstance<Editable<*>>())

    override suspend fun deserializeState(json: String) {
        removeAll()
        // TODO: Weird things happen at this point once we try to restore more than 20000 Actors. Singletons constructors get invoked again.
        add(actors = engineImpl.serializationManager.deserializeActors(json).toTypedArray())
    }

    override fun findVisibleInstancesWithBoundsInPosition(position: SceneOffset) = visibleActorsWithinViewport.value
        .filter { it.occupiesPosition(position) }

    override fun findVisibleInstancesWithPivotsAroundPosition(position: SceneOffset, range: Float) = visibleActors.value
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
}