package com.pandulapeter.kubriko.implementation.manager

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.Identifiable
import com.pandulapeter.kubriko.actor.traits.LayerAware
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.KubrikoImpl
import com.pandulapeter.kubriko.implementation.extensions.distinctUntilChangedWithDelay
import com.pandulapeter.kubriko.implementation.extensions.div
import com.pandulapeter.kubriko.implementation.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class ActorManagerImpl(
    initialActors: List<Actor>,
    private val invisibleActorMinimumRefreshTimeInMillis: Long, // TODO: Feels hacky
) : ActorManager() {

    private lateinit var metadataManager: MetadataManager
    private lateinit var viewportManager: ViewportManagerImpl
    private lateinit var stateManager: StateManager
    private val _allActors = MutableStateFlow<ImmutableList<Actor>>(initialActors.toPersistentList())
    override val allActors = _allActors.asStateFlow()
    val layerIndices by autoInitializingLazy {
        _allActors.map { actors -> actors.filterIsInstance<LayerAware>().groupBy { it.layerIndex }.keys.sortedByDescending { it }.toImmutableList() }
            .asStateFlow(persistentListOf())
    }
    private val dynamicActors by autoInitializingLazy {
        _allActors.map { actors -> actors.filterIsInstance<Dynamic>().toImmutableList() }.asStateFlow(persistentListOf())
    }
    private val visibleActors by autoInitializingLazy {
        _allActors.map { actors -> actors.filterIsInstance<Visible>().sortedByDescending { it.drawingOrder }.toImmutableList() }.asStateFlow(persistentListOf())
    }
    val overlayActors by autoInitializingLazy {
        _allActors.map { actors -> actors.filterIsInstance<Overlay>().sortedByDescending { it.overlayDrawingOrder }.toImmutableList() }.asStateFlow(persistentListOf())
    }
    override val visibleActorsWithinViewport by lazy {
        combine(
            metadataManager.runtimeInMilliseconds.distinctUntilChangedWithDelay(invisibleActorMinimumRefreshTimeInMillis),
            visibleActors,
            viewportManager.size,
            viewportManager.cameraPosition,
            viewportManager.scaleFactor,
        ) { _, allVisibleActors, viewportSize, viewportCenter, scaleFactor ->
            allVisibleActors
                .filter {
                    it.body.axisAlignedBoundingBox.isWithinViewportBounds(
                        scaledHalfViewportSize = SceneSize(viewportSize / (scaleFactor * 2)),
                        viewportCenter = viewportCenter,
                        viewportEdgeBuffer = viewportManager.viewportEdgeBuffer,
                    )
                }
                .toImmutableList()
        }.asStateFlow(persistentListOf())
    }

    override fun onInitialize(kubriko: Kubriko) {
        metadataManager = (kubriko as KubrikoImpl).metadataManager
        stateManager = kubriko.stateManager
        viewportManager = kubriko.viewportManager
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        if (stateManager.isRunning.value) {
            dynamicActors.value.forEach {
                // TODO: Reduce update frequency for Positionable Dynamic actors that are not within the viewport
                it.update(deltaTimeInMillis)
            }
        }
    }

    private fun flattenActors(actors: List<Actor>): List<Actor> = actors.flatMap { actor ->
        if (actor is Group) flattenActors(actor.actors) + actor
        else listOf(actor)
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun add(vararg actors: Actor) = _allActors.update { currentActors ->
        val newActors = flattenActors(actors.toList())
        val uniqueNewActorTypes = newActors.filterIsInstance<Unique>().map { it::class }.toSet()
        val filteredCurrentActors = currentActors.filterNot { it::class in uniqueNewActorTypes }
        newActors.filterIsInstance<Identifiable>().onEach { if (it.name == null) it.name = Uuid.random().toString() }
        newActors.forEach { it.onAdded(scope as Kubriko) }
        (filteredCurrentActors + newActors).toImmutableList()
    }

    override fun add(actors: Collection<Actor>) = add(actors = actors.toTypedArray())

    override fun remove(vararg actors: Actor) {
        val flattenedActors = flattenActors(actors.toList())
        _allActors.update { currentActors ->
            currentActors.filterNot { it in flattenedActors }.toImmutableList()
        }
        flattenedActors.forEach { it.onRemoved() }
    }

    override fun remove(actors: Collection<Actor>) = remove(actors = actors.toTypedArray())

    override fun removeAll() {
        val currentActors = _allActors.value
        _allActors.update { persistentListOf() }
        currentActors.forEach { it.onRemoved() }
    }
}