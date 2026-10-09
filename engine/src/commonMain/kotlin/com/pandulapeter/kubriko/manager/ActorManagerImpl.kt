/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.LayerAware
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class ActorManagerImpl(
    private val initialActors: List<Actor>,
    private val shouldUpdateActorsWhileNotRunning: Boolean,
    private val shouldPutFarAwayActorsToSleep: Boolean,
    private val farAwayActorSleepMargin: SceneUnit?,
    private val invisibleActorMinimumRefreshTimeInMillis: Long,
    private val shouldComposeLayers: Boolean,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : ActorManager(isLoggingEnabled, instanceNameForLogging) {
    private lateinit var metadataManager: MetadataManagerImpl
    private lateinit var viewportManager: ViewportManagerImpl
    private lateinit var stateManager: StateManager
    private val batchProcessor = ActorBatchProcessor(shouldComposeLayers) { message, details ->
        log(message = message, details = details)
    }
    override val allActors = batchProcessor.allActors
    private val _visibleActorsWithinViewport = MutableStateFlow<ImmutableList<Visible>>(persistentListOf())
    override val visibleActorsWithinViewport = _visibleActorsWithinViewport.asStateFlow()
    private val _activeDynamicActors = MutableStateFlow<ImmutableList<Dynamic>>(persistentListOf())
    override val activeDynamicActors = _activeDynamicActors.asStateFlow()
    private lateinit var kubrikoImpl: KubrikoImpl
    private lateinit var culler: ActorCuller
    /**
     * The distinct layer indices, sorted with `null` first. A headless instance never composes a layer, so it doesn't
     * follow every change of its actor list.
     */
    private val layerIndices by autoInitializingLazy {
        if (!shouldComposeLayers) MutableStateFlow<ImmutableList<Int?>>(persistentListOf()).asStateFlow() else allActors
            .map { actors ->
                // Only the distinct indices are needed, not every actor grouped under its own.
                val indices = HashSet<Int?>()
                for (actor in actors) {
                    if (actor is LayerAware) indices.add(actor.layerIndex)
                }
                indices.sortedWith(nullsFirst(naturalOrder())).toImmutableList()
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }

    /**
     * Mirror of the published active Dynamic list for the update loop: iterating the persistent list directly would
     * allocate a trie iterator every frame, while the ArrayList mirror is indexable in O(1). Refilled only when the
     * published list reference changes; tick-thread-private.
     */
    private val activeDynamicMirror = ArrayList<Dynamic>()
    private var lastMirroredActiveDynamicActors: ImmutableList<Dynamic>? = null

    override fun onInitialize(kubriko: Kubriko) {
        kubrikoImpl = kubriko as KubrikoImpl
        metadataManager = kubriko.metadataManager
        stateManager = kubriko.stateManager
        viewportManager = kubriko.viewportManager
        culler = ActorCuller(
            viewportManager = viewportManager,
            metadataManager = metadataManager,
            farAwayActorSleepMargin = farAwayActorSleepMargin,
            invisibleActorMinimumRefreshTimeInMillis = invisibleActorMinimumRefreshTimeInMillis,
            shouldComposeLayers = shouldComposeLayers,
            dynamicActors = batchProcessor.dynamicActors,
            visibleActors = batchProcessor.visibleActors,
            overlayActors = batchProcessor.overlayActors,
            _visibleActorsWithinViewport = _visibleActorsWithinViewport,
            _activeDynamicActors = _activeDynamicActors,
        )
        add(initialActors)
    }

    internal fun startProcessingOperations() = batchProcessor.start(kubrikoImpl, scope)

    override fun onDispose() = batchProcessor.dispose()

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        // Refreshed before the update loop, so that a batch applied since the previous tick is already reflected in
        // the actors updated by this one.
        val didCullDynamicActorsBeforeUpdate =
            culler.refreshActiveDynamicActorsBeforeUpdate(shouldPutFarAwayActorsToSleep)

        if (shouldUpdateActorsWhileNotRunning || stateManager.isRunning.value) {
            val currentActiveDynamicActors = activeDynamicActors.value
            if (currentActiveDynamicActors !== lastMirroredActiveDynamicActors) {
                lastMirroredActiveDynamicActors = currentActiveDynamicActors
                activeDynamicMirror.clear()
                activeDynamicMirror.addAll(currentActiveDynamicActors)
            }
            for (i in activeDynamicMirror.indices) {
                if (kubrikoImpl.isDisposedInternal) return
                activeDynamicMirror[i].update(deltaTimeInMilliseconds)
            }
            if (kubrikoImpl.isDisposedInternal) return
        }

        culler.refreshAfterUpdate(shouldPutFarAwayActorsToSleep, didCullDynamicActorsBeforeUpdate)
    }

    override fun add(vararg actors: Actor) {
        if (actors.isEmpty()) return
        batchProcessor.add(actors.toList())
    }

    override fun add(actors: Collection<Actor>) {
        if (actors.isEmpty()) return
        batchProcessor.add(actors.toList())
    }

    override fun remove(vararg actors: Actor) {
        if (actors.isEmpty()) return
        batchProcessor.remove(actors.toList())
    }

    override fun remove(actors: Collection<Actor>) {
        if (actors.isEmpty()) return
        batchProcessor.remove(actors.toList())
    }

    override fun removeAll() = batchProcessor.removeAll()

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (!shouldComposeLayers) return
        val gameTime = metadataManager.gameTime
        Box(
            modifier = kubrikoImpl.managers.fold(Modifier.clipToBounds()) { modifierToProcess, manager ->
                manager.processModifierInternal(modifierToProcess, null, gameTime)
            },
        ) {
            Layers(
                layerIndices = layerIndices,
                gameTime = gameTime,
                managers = kubrikoImpl.managers,
                viewportManager = viewportManager,
                culler = culler,
            )
        }
    }
}
