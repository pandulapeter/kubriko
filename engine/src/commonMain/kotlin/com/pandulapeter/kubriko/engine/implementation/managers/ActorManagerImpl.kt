package com.pandulapeter.kubriko.engine.implementation.managers

import com.pandulapeter.kubriko.engine.implementation.KubrikoImpl
import com.pandulapeter.kubriko.engine.implementation.extensions.isAroundPosition
import com.pandulapeter.kubriko.engine.implementation.extensions.isVisible
import com.pandulapeter.kubriko.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.kubriko.engine.managers.ActorManager
import com.pandulapeter.kubriko.engine.traits.Dynamic
import com.pandulapeter.kubriko.engine.traits.Editable
import com.pandulapeter.kubriko.engine.traits.Identifiable
import com.pandulapeter.kubriko.engine.traits.Overlay
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class ActorManagerImpl(
    private val engineImpl: KubrikoImpl,
) : ActorManager {

    private val _allActors = MutableStateFlow(emptyList<Any>())
    override val allActors = _allActors.asStateFlow()
    val dynamicActors = _allActors
        .map { actors -> actors.filterIsInstance<Dynamic>() }
        .stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    private val visibleActors = _allActors
        .map { actors -> actors.filterIsInstance<Visible>() }
        .stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    val overlayActors = _allActors
        .map { actors -> actors.filterIsInstance<Overlay>() }
        .stateIn(engineImpl, SharingStarted.Eagerly, emptyList())
    override val visibleActorsWithinViewport = combine(
        engineImpl.metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
        visibleActors,
        engineImpl.viewportManager.size,
        engineImpl.viewportManager.center,
        engineImpl.viewportManager.scaleFactor,
    ) { _, allVisibleActors, viewportSize, viewportCenter, viewportScaleFactor ->
        allVisibleActors
            .filter {
                it.isVisible(
                    scaledHalfViewportSize = SceneSize(viewportSize / (viewportScaleFactor * 2)),
                    viewportCenter = viewportCenter,
                    viewportScaleFactor = viewportScaleFactor,
                )
            }
            .sortedByDescending { it.drawingOrder }
    }.stateIn(engineImpl, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalUuidApi::class)
    override fun add(vararg actors: Any) = _allActors.update { currentActors ->
        val uniqueActors = actors.filterIsInstance<Unique>().map { it::class }.toSet()
        val filteredCurrentActors = currentActors.filterNot { it::class in uniqueActors }
        val currentIdentifiableActors = filteredCurrentActors.filterIsInstance<Identifiable>()
        val newIdentifiableActors = actors
            .filterIsInstance<Identifiable>()
            .onEach { if (it.id == null) it.id = Uuid.random().toString() }
            .distinctBy { it.id }
        val identifiableActors = (currentIdentifiableActors + newIdentifiableActors).distinctBy { it.id }
        val nonIdentifiableActors = (filteredCurrentActors + actors).filterNot { it is Identifiable }
        nonIdentifiableActors + identifiableActors
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