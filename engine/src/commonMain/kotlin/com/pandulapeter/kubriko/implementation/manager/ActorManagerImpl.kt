package com.pandulapeter.kubriko.implementation.manager

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Identifiable
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.KubrikoImpl
import com.pandulapeter.kubriko.implementation.extensions.isVisible
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
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

internal class ActorManagerImpl : ActorManager() {

    private lateinit var metadataManager: MetadataManager
    private lateinit var viewportManager: ViewportManager
    private lateinit var stateManager: StateManager
    private val _allActors = MutableStateFlow(emptyList<Actor>())
    override val allActors = _allActors.asStateFlow()
    private val dynamicActors by lazy {
        _allActors
            .map { actors -> actors.filterIsInstance<Dynamic>() }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    private val visibleActors by lazy {
        _allActors
            .map { actors -> actors.filterIsInstance<Visible>() }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    val overlayActors by lazy {
        _allActors
            .map { actors -> actors.filterIsInstance<Overlay>() }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    override val visibleActorsWithinViewport by lazy {
        combine(
            metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
            visibleActors,
            viewportManager.size,
            viewportManager.cameraPosition,
            viewportManager.scaleFactor,
        ) { _, allVisibleActors, viewportSize, viewportCenter, viewportScaleFactor ->
            allVisibleActors
                .filter {
                    it.isVisible(
                        scaledHalfViewportSize = SceneSize(viewportSize / (viewportScaleFactor * 2)),
                        viewportCenter = viewportCenter,
                        viewportScaleFactor = viewportScaleFactor,
                    )
                }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    override fun onInitialize(kubriko: Kubriko) {
        metadataManager = (kubriko as KubrikoImpl).metadataManager
        stateManager = kubriko.stateManager
        viewportManager = kubriko.viewportManager
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        if (stateManager.isRunning.value) {
            dynamicActors.value.forEach { it.update(deltaTimeInMillis) }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun add(vararg actors: Actor) = _allActors.update { currentActors ->
        val uniqueActors = actors.filterIsInstance<Unique>().map { it::class }.toSet()
        val filteredCurrentActors = currentActors.filterNot { it::class in uniqueActors }
        actors.filterIsInstance<Identifiable>().onEach { if (it.name == null) it.name = Uuid.random().toString() }
        actors.forEach { it.onAdd(scope as Kubriko) }
        filteredCurrentActors + actors
    }

    override fun remove(vararg actors: Actor) = _allActors.update { currentValue ->
        actors.forEach { it.onRemove() }
        currentValue.filterNot { it in actors }
    }

    override fun removeAll() = _allActors.update { currentValue ->
        currentValue.forEach { it.onRemove() }
        emptyList()
    }
}