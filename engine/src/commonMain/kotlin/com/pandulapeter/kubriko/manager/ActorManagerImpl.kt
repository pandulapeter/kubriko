/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
import androidx.compose.ui.geometry.Size
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
import com.pandulapeter.kubriko.extensions.distinctUntilChangedWithDelay
import com.pandulapeter.kubriko.extensions.div
import com.pandulapeter.kubriko.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.extensions.minus
import com.pandulapeter.kubriko.extensions.transformForViewport
import com.pandulapeter.kubriko.extensions.transformViewport
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
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
    private val shouldUpdateActorsWhileNotRunning: Boolean,
    private val shouldPutFarAwayActorsToSleep: Boolean,
    private val invisibleActorMinimumRefreshTimeInMillis: Long, // TODO: Feels hacky
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
        _allActors.map { actors -> actors.filterIsInstance<LayerAware>().groupBy { it.layerIndex }.keys.sortedByDescending { it }.toImmutableList() }
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
            metadataManager.activeRuntimeInMilliseconds.distinctUntilChangedWithDelay(invisibleActorMinimumRefreshTimeInMillis),
            visibleActors,
            viewportManager.size,
            viewportManager.cameraPosition,
            viewportManager.scaleFactor,
        ) { _, allVisibleActors, viewportSize, viewportCenter, scaleFactor ->
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
        }.asStateFlow(persistentListOf())
    }
    override val activeDynamicActors by lazy {
        if (shouldPutFarAwayActorsToSleep) combine(
            dynamicActors,
            viewportManager.size,
            viewportManager.cameraPosition,
            viewportManager.topLeft,
            viewportManager.bottomRight,
            viewportManager.scaleFactor,
        ) {
            @Suppress("UNCHECKED_CAST")
            val allDynamicActors = it[0] as ImmutableList<Dynamic>
            val viewportSize = it[1] as Size
            val viewportCenter = it[2] as SceneOffset
            val viewportTopLeft = it[3] as SceneOffset
            val viewportBottomRight = it[4] as SceneOffset
            val scaleFactor = it[5] as Scale
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
        }.asStateFlow(persistentListOf()) else dynamicActors
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

    private fun flattenActors(actors: List<Actor>): List<Actor> = actors.flatMap { actor ->
        if (actor is Group) flattenActors(actor.actors.filterNot { it === actor }) + actor
        else listOf(actor)
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun add(vararg actors: Actor) = _allActors.update { currentActors ->
        log(
            message = "Adding ${actors.size} new Actors to ${currentActors.size} existing ones",
            importance = Logger.Importance.LOW,
        )
        val newActors = flattenActors(actors.toList())
        val uniqueNewActorTypes = newActors.filterIsInstance<Unique>().map { it::class }.toSet()
        val filteredCurrentActors = currentActors.filterNot { it::class in uniqueNewActorTypes }
        newActors.filterIsInstance<Identifiable>().onEach { if (it.name == null) it.name = Uuid.random().toString() }
        newActors.forEach { it.onAdded(scope as Kubriko) }
        (filteredCurrentActors + newActors).toImmutableList()
    }

    override fun add(actors: Collection<Actor>) = add(actors = actors.toTypedArray())

    override fun remove(vararg actors: Actor) {
        if (actors.isNotEmpty()) {
            log(
                message = "Removing ${actors.size} Actors",
                importance = Logger.Importance.LOW,
            )
            val flattenedActors = flattenActors(actors.toList())
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
    ) = viewportManager.cameraPosition.value.let { viewportCenter ->
        Canvas(
            modifier = if (layerIndex == null) {
                Modifier.fillMaxSize().clipToBounds()
            } else {
                kubrikoImpl.managers.fold(Modifier.fillMaxSize().clipToBounds()) { modifierToProcess, manager ->
                    manager.processModifierInternal(modifierToProcess, layerIndex)
                }
            },
            onDraw = {
                @Suppress("UNUSED_EXPRESSION") gameTime  // This line invalidates the Canvas, causing a refresh on every frame
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportCenter = viewportCenter,
                            shiftedViewportOffset = (viewportManager.size.value / 2f) - viewportCenter,
                            viewportScaleFactor = viewportManager.scaleFactor.value,
                        )
                    },
                    drawBlock = {
                        visibleActorsWithinViewport.value
                            .filter { it.isVisible && it.layerIndex == layerIndex }
                            .sortedByDescending { it.drawingOrder }
                            .forEach { visible ->
                                withTransform(
                                    transformBlock = { visible.body.transformForViewport(this) },
                                    drawBlock = {
                                        with(visible) {
                                            if (shouldClip) {
                                                clipRect(
                                                    left = -ACTOR_CLIPPING_BORDER,
                                                    top = -ACTOR_CLIPPING_BORDER,
                                                    right = body.size.width.raw + ACTOR_CLIPPING_BORDER,
                                                    bottom = body.size.height.raw + ACTOR_CLIPPING_BORDER,
                                                ) {
                                                    draw()
                                                }
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
                    .filter { it.layerIndex == layerIndex }
                    .sortedByDescending { it.overlayDrawingOrder }
                    .forEach { with(it) { drawToViewport() } }
            }
        )
    }

    override fun onDispose() {
        _allActors.value = persistentListOf()
    }

    companion object {
        // TODO: Probably shouldn't be necessary
        private const val ACTOR_CLIPPING_BORDER = 20f
    }
}