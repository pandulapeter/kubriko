package com.pandulapeter.kubriko.implementation.managers

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Identifiable
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.KubrikoImpl
import com.pandulapeter.kubriko.implementation.extensions.isAroundPosition
import com.pandulapeter.kubriko.implementation.extensions.isVisible
import com.pandulapeter.kubriko.managers.ActorManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
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
    engineImpl: KubrikoImpl,
) : ActorManager {

    private val _allActors = MutableStateFlow(emptyList<Actor>())
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
    }.stateIn(engineImpl, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalUuidApi::class)
    override fun add(vararg actors: Actor) = _allActors.update { currentActors ->
        val uniqueActors = actors.filterIsInstance<Unique>().map { it::class }.toSet()
        val filteredCurrentActors = currentActors.filterNot { it::class in uniqueActors }
        actors.filterIsInstance<Identifiable>().onEach { if (it.name == null) it.name = Uuid.random().toString() }
        filteredCurrentActors + actors
    }

    override fun remove(vararg actors: Actor) = _allActors.update { currentValue ->
        currentValue.filterNot { it in actors }
    }

    override fun removeAll() = _allActors.update { emptyList() }

    override fun findVisibleActorsWithPivotsAroundPosition(position: SceneOffset, range: Float) = visibleActors.value
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
}