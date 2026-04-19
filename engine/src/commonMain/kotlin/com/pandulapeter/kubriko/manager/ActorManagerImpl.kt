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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Disposable
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.Identifiable
import com.pandulapeter.kubriko.actor.traits.LayerAware
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.distinctUntilChangedWithDelay
import com.pandulapeter.kubriko.helpers.extensions.div
import com.pandulapeter.kubriko.helpers.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.helpers.extensions.minus
import com.pandulapeter.kubriko.helpers.extensions.transformForViewport
import com.pandulapeter.kubriko.helpers.extensions.transformViewport
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class)
internal class ActorManagerImpl(
    initialActors: List<Actor>,
    private val shouldUpdateActorsWhileNotRunning: Boolean,
    private val shouldPutFarAwayActorsToSleep: Boolean,
    private val invisibleActorMinimumRefreshTimeInMillis: Long,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : ActorManager(isLoggingEnabled, instanceNameForLogging) {
    private lateinit var metadataManager: MetadataManagerImpl
    private lateinit var viewportManager: ViewportManagerImpl
    private lateinit var stateManager: StateManager
    private val _allActors = MutableStateFlow<ImmutableList<Actor>>(initialActors.toPersistentList())
    override val allActors = _allActors.asStateFlow()
    private lateinit var kubrikoImpl: KubrikoImpl

    private val layerIndices by autoInitializingLazy {
        _allActors.map { actors -> actors.filterIsInstance<LayerAware>().groupBy { it.layerIndex }.keys.sortedBy { it }.toImmutableList() }
            .asStateFlow(persistentListOf())
    }
    private val dynamicActors by autoInitializingLazy {
        _allActors.map { actors -> actors.filterIsInstance<Dynamic>().toImmutableList() }.asStateFlow(persistentListOf())
    }
    private val visibleActors by autoInitializingLazy {
        _allActors.map { actors -> actors.filterIsInstance<Visible>().toImmutableList() }.asStateFlow(persistentListOf())
    }
    private val overlayActors by autoInitializingLazy {
        _allActors.map { actors -> actors.filterIsInstance<Overlay>().toImmutableList() }
            .asStateFlow(persistentListOf())
    }

    override val visibleActorsWithinViewport by lazy {
        combine(
            visibleActors.debounce(8L),
            metadataManager.activeRuntimeInMilliseconds
                .distinctUntilChangedWithDelay(invisibleActorMinimumRefreshTimeInMillis)
                .onStart { emit(-1L) } // Forces initial evaluation even if paused
        ) { allVisibleActors, _ ->
            val viewportSize = viewportManager.size.value
            val viewportCenter = viewportManager.cameraPosition.value
            val scaleFactor = viewportManager.scaleFactor.value
            val scaledHalfViewportSize = SceneSize(viewportSize / (scaleFactor * 2))
            allVisibleActors
                .filter {
                    it.body.axisAlignedBoundingBox.isWithinViewportBounds(
                        scaledHalfViewportSize = scaledHalfViewportSize,
                        viewportCenter = viewportCenter,
                        viewportEdgeBuffer = viewportManager.viewportEdgeBuffer,
                    )
                }
                .toImmutableList()
        }
            .flowOn(Dispatchers.Default) // Execute the heavy spatial math on a background CPU thread
            .asStateFlow(persistentListOf())
    }

    override val activeDynamicActors by lazy {
        if (shouldPutFarAwayActorsToSleep) {
            combine(
                dynamicActors.debounce(8),
                metadataManager.activeRuntimeInMilliseconds
                    .distinctUntilChangedWithDelay(invisibleActorMinimumRefreshTimeInMillis)
                    .onStart { emit(-1L) }
            ) { allDynamicActors, _ ->
                val viewportSize = viewportManager.size.value
                val viewportCenter = viewportManager.cameraPosition.value
                val viewportTopLeft = viewportManager.topLeft.value
                val viewportBottomRight = viewportManager.bottomRight.value
                val scaleFactor = viewportManager.scaleFactor.value
                val edgeBuffer = minOf(viewportBottomRight.x - viewportTopLeft.x, viewportBottomRight.y - viewportTopLeft.y) / 2
                val scaledHalfViewportSize = SceneSize(viewportSize / (scaleFactor * 2))

                allDynamicActors
                    .filter { actor ->
                        if (!actor.isAlwaysActive && actor is Positionable) {
                            actor.body.axisAlignedBoundingBox.isWithinViewportBounds(
                                scaledHalfViewportSize = scaledHalfViewportSize,
                                viewportCenter = viewportCenter,
                                viewportEdgeBuffer = edgeBuffer,
                            )
                        } else {
                            true
                        }
                    }
                    .toImmutableList()
            }
                .flowOn(Dispatchers.Default)
                .asStateFlow(persistentListOf())
        } else dynamicActors
    }

    override fun onInitialize(kubriko: Kubriko) {
        kubrikoImpl = kubriko as KubrikoImpl
        metadataManager = kubriko.metadataManager
        stateManager = kubriko.stateManager
        viewportManager = kubriko.viewportManager
        allActors.value.forEach { it.onAdded(kubriko) }
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (shouldUpdateActorsWhileNotRunning || stateManager.isRunning.value) {
            activeDynamicActors.value.forEach { it.update(deltaTimeInMilliseconds) }
        }
    }

    private fun flattenActors(initialActors: List<Actor>): List<Actor> {
        val result = ArrayList<Actor>()
        val queue = ArrayDeque(initialActors)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)

            if (current is Group) {
                for (child in current.actors) {
                    if (child !== current) {
                        queue.addLast(child)
                    }
                }
            }
        }
        return result
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun add(vararg actors: Actor) {
        if (actors.isEmpty()) return
        val flattened = flattenActors(actors.asList())
        val latestUniqueByClass = LinkedHashMap<KClass<out Actor>, Actor>()
        val nonUnique = ArrayList<Actor>(flattened.size)
        for (a in flattened) {
            if (a is Unique) {
                latestUniqueByClass[a::class] = a // overwrites -> "latest wins"
            } else {
                nonUnique.add(a)
            }
        }
        val newActors = ArrayList<Actor>(nonUnique.size + latestUniqueByClass.size).apply {
            addAll(nonUnique)
            addAll(latestUniqueByClass.values)
        }
        for (a in newActors) {
            if (a is Identifiable && a.name == null) a.name = Uuid.random().toString()
        }
        val uniqueTypesToReplace = latestUniqueByClass.keys
        _allActors.update { current ->
            val filteredCurrent = if (uniqueTypesToReplace.isEmpty()) current
            else current.filterNot { it::class in uniqueTypesToReplace }
            (filteredCurrent + newActors).toImmutableList()
        }
        newActors.forEach { it.onAdded(scope as Kubriko) }
    }

    override fun add(actors: Collection<Actor>) = add(actors = actors.toTypedArray())

    override fun remove(vararg actors: Actor) {
        if (actors.isNotEmpty()) {
            val flattenedActors = flattenActors(actors.toList()).asReversed()
            _allActors.update { currentActors ->
                currentActors.filterNot { it in flattenedActors }.toImmutableList()
            }
            flattenedActors.forEach {
                (it as? Disposable)?.dispose()
                it.onRemoved()
            }
        }
    }

    override fun remove(actors: Collection<Actor>) = remove(actors = actors.toTypedArray())

    override fun removeAll() {
        val currentActors = _allActors.value
        _allActors.update { persistentListOf() }
        currentActors.forEach { it.onRemoved() }
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) = Box(
        modifier = if (isInitialized.collectAsState().value) kubrikoImpl.managers.fold(Modifier.clipToBounds()) { modifierToProcess, manager ->
            manager.processModifierInternal(modifierToProcess, null)
        } else Modifier.clipToBounds(),
    ) {
        val gameTime = metadataManager.totalRuntimeInMilliseconds.collectAsState().value
        layerIndices.value.forEach { layerIndex ->
            Layer(
                gameTime = gameTime,
                layerIndex = layerIndex,
            )
        }
    }

    @Composable
    private fun Layer(
        gameTime: Long,
        layerIndex: Int?,
    ) {
        Canvas(
            modifier = if (layerIndex == null) {
                Modifier.fillMaxSize().clipToBounds()
            } else {
                kubrikoImpl.managers.fold(Modifier.fillMaxSize().clipToBounds()) { modifierToProcess, manager ->
                    manager.processModifierInternal(modifierToProcess, layerIndex)
                }
            },
            onDraw = {
                @Suppress("UNUSED_EXPRESSION") gameTime  // Invalidates the Canvas, causing a refresh on every frame
                val viewportCenter = viewportManager.cameraPosition.value
                val viewportSize = viewportManager.size.value
                val scaleFactor = viewportManager.scaleFactor.value
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportCenter = viewportCenter,
                            shiftedViewportOffset = (viewportSize / 2f) - viewportCenter,
                            viewportScaleFactor = scaleFactor,
                        )
                    },
                    drawBlock = {
                        visibleActorsWithinViewport.value
                            .filter { it.isVisible && it.layerIndex == layerIndex }
                            .sortedWith { a, b ->
                                val orderA = a.drawingOrder
                                val orderB = b.drawingOrder
                                if (orderA.isNaN()) return@sortedWith 1
                                if (orderB.isNaN()) return@sortedWith -1
                                compareValues(orderB as Comparable<*>, orderA as Comparable<*>)
                            }
                            .forEach { visible ->
                                withTransform(
                                    transformBlock = { visible.body.transformForViewport(this) },
                                    drawBlock = {
                                        with(visible) {
                                            if (shouldClip) {
                                                clipRect(
                                                    left = 0f,
                                                    top = 0f,
                                                    right = body.size.width.raw,
                                                    bottom = body.size.height.raw,
                                                ) { draw() }
                                            } else {
                                                draw()
                                            }
                                        }
                                    }
                                )
                            }
                    }
                )
                overlayActors.value
                    .sortedWith { a, b ->
                        val orderA = a.overlayDrawingOrder
                        val orderB = b.overlayDrawingOrder
                        if (orderA.isNaN()) return@sortedWith 1
                        if (orderB.isNaN()) return@sortedWith -1
                        compareValues(orderB as Comparable<*>, orderA as Comparable<*>)
                    }
                    .forEach { overlay ->
                        if (overlay.layerIndex == layerIndex) {
                            with(overlay) { drawToViewport() }
                        }
                    }
            }
        )
    }
}