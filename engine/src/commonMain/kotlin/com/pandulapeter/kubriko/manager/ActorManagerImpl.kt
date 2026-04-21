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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class)
internal class ActorManagerImpl(
    private val initialActors: List<Actor>,
    private val shouldUpdateActorsWhileNotRunning: Boolean,
    private val shouldPutFarAwayActorsToSleep: Boolean,
    private val invisibleActorMinimumRefreshTimeInMillis: Long,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : ActorManager(isLoggingEnabled, instanceNameForLogging) {
    private lateinit var metadataManager: MetadataManagerImpl
    private lateinit var viewportManager: ViewportManagerImpl
    private lateinit var stateManager: StateManager
    private val _allActors = MutableStateFlow<ImmutableList<Actor>>(persistentListOf())
    override val allActors = _allActors.asStateFlow()
    private lateinit var kubrikoImpl: KubrikoImpl
    private val operationChannel = Channel<Operation>(Channel.UNLIMITED)
    private val drawingOrderComparator = Comparator<Visible> { a, b ->
        val orderA = a.drawingOrder
        val orderB = b.drawingOrder
        if (orderA.isNaN()) return@Comparator 1
        if (orderB.isNaN()) return@Comparator -1
        compareValues(orderB as Comparable<*>, orderA as Comparable<*>)
    }
    private val overlayDrawingOrderComparator = Comparator<Overlay> { a, b ->
        val orderA = a.overlayDrawingOrder
        val orderB = b.overlayDrawingOrder
        if (orderA.isNaN()) return@Comparator 1
        if (orderB.isNaN()) return@Comparator -1
        compareValues(orderB as Comparable<*>, orderA as Comparable<*>)
    }
    private val layerIndices by autoInitializingLazy {
        _allActors
            .map { actors -> actors.filterIsInstance<LayerAware>().groupBy { it.layerIndex }.keys.sortedBy { it }.toImmutableList() }
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }
    private val dynamicActors by autoInitializingLazy {
        _allActors
            .map { actors -> actors.filterIsInstance<Dynamic>().toImmutableList() }
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }
    private val visibleActors by autoInitializingLazy {
        _allActors
            .map { actors -> actors.filterIsInstance<Visible>().toImmutableList() }
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }
    private val overlayActors by autoInitializingLazy {
        _allActors
            .map { actors -> actors.filterIsInstance<Overlay>().toImmutableList() }
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }

    override val visibleActorsWithinViewport by lazy {
        combine(
            visibleActors,
            metadataManager.activeRuntimeInMilliseconds
                .distinctUntilChangedWithDelay(invisibleActorMinimumRefreshTimeInMillis)
                .onStart { emit(-1L) },
            viewportManager.cameraPosition.debounce(8L),
            viewportManager.size.debounce(8L),
            viewportManager.scaleFactor.debounce(8L),
        ) { allVisibleActors, _, viewportCenter, viewportSize, scaleFactor ->
            val scaledHalfViewportSize = SceneSize(viewportSize / (scaleFactor * 2f))
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
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }

    override val activeDynamicActors by lazy {
        if (shouldPutFarAwayActorsToSleep) {
            combine(
                dynamicActors,
                metadataManager.activeRuntimeInMilliseconds
                    .distinctUntilChangedWithDelay(invisibleActorMinimumRefreshTimeInMillis)
                    .onStart { emit(-1L) },
                viewportManager.cameraPosition.debounce(8L),
                viewportManager.size.debounce(8L),
                viewportManager.scaleFactor.debounce(8L)
            ) { allDynamicActors, _, viewportCenter, viewportSize, scaleFactor ->
                val viewportTopLeft = viewportManager.topLeft.value
                val viewportBottomRight = viewportManager.bottomRight.value
                val edgeBuffer = minOf(viewportBottomRight.x - viewportTopLeft.x, viewportBottomRight.y - viewportTopLeft.y) / 2f
                val scaledHalfViewportSize = SceneSize(viewportSize / (scaleFactor * 2f))
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
                .asStateFlowOnMainThread(persistentListOf())
        } else dynamicActors
    }

    override fun onInitialize(kubriko: Kubriko) {
        kubrikoImpl = kubriko as KubrikoImpl
        metadataManager = kubriko.metadataManager
        stateManager = kubriko.stateManager
        viewportManager = kubriko.viewportManager
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val firstOp = operationChannel.receive()
                    val batch = mutableListOf(firstOp)
                    while (true) {
                        val op = operationChannel.tryReceive().getOrNull() ?: break
                        batch.add(op)
                    }
                    processBatch(batch)
                } catch (_: Exception) {
                }
            }
        }
        add(initialActors)
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
    private fun processBatch(batch: List<Operation>) {
        var workingList = _allActors.value
        val newlyAdded = LinkedHashSet<Actor>()
        val newlyRemoved = LinkedHashSet<Actor>()
        for (op in batch) {
            when (op) {
                is Operation.Add -> {
                    val flattened = flattenActors(op.actors)
                    val latestUniqueByClass = LinkedHashMap<KClass<out Actor>, Actor>()
                    val nonUnique = ArrayList<Actor>(flattened.size)
                    for (a in flattened) {
                        if (a is Unique) latestUniqueByClass[a::class] = a
                        else nonUnique.add(a)
                    }
                    val newActors = ArrayList<Actor>(nonUnique.size + latestUniqueByClass.size).apply {
                        addAll(nonUnique)
                        addAll(latestUniqueByClass.values)
                    }
                    for (a in newActors) {
                        if (a is Identifiable && a.name == null) a.name = Uuid.random().toString()
                    }
                    val uniqueTypesToReplace = latestUniqueByClass.keys
                    workingList.forEach {
                        if (it::class in uniqueTypesToReplace) {
                            newlyRemoved.add(it)
                            newlyAdded.remove(it)
                        }
                    }
                    workingList = (workingList.filterNot { it::class in uniqueTypesToReplace } + newActors).toImmutableList()
                    newActors.forEach {
                        newlyAdded.add(it)
                        newlyRemoved.remove(it)
                    }
                }

                is Operation.Remove -> {
                    val flattenedActors = flattenActors(op.actors).asReversed()
                    val validRemovals = flattenedActors.filter { workingList.contains(it) }
                    if (validRemovals.isNotEmpty()) {
                        val removalCollection = if (validRemovals.size > 10) validRemovals.toHashSet() else validRemovals
                        workingList = workingList.filterNot { it in removalCollection }.toImmutableList()
                        validRemovals.forEach {
                            newlyRemoved.add(it)
                            newlyAdded.remove(it)
                        }
                    }
                }

                is Operation.RemoveAll -> {
                    workingList.forEach {
                        newlyRemoved.add(it)
                        newlyAdded.remove(it)
                    }
                    workingList = persistentListOf()
                }
            }
        }
        if (newlyAdded.isNotEmpty()) {
            newlyAdded.forEach { it.onAdded(kubrikoImpl) }
        }
        _allActors.value = workingList
        if (newlyRemoved.isNotEmpty()) {
            newlyRemoved.forEach {
                (it as? Disposable)?.dispose()
                it.onRemoved()
            }
        }
    }

    override fun add(vararg actors: Actor) {
        if (actors.isEmpty()) return
        scope.launch {
            operationChannel.send(Operation.Add(actors.toList()))
        }
    }

    override fun add(actors: Collection<Actor>) = add(actors = actors.toTypedArray())

    override fun remove(vararg actors: Actor) {
        if (actors.isEmpty()) return
        scope.launch {
            operationChannel.send(Operation.Remove(actors.toList()))
        }
    }

    override fun remove(actors: Collection<Actor>) = remove(actors = actors.toTypedArray())

    override fun removeAll() {
        scope.launch {
            operationChannel.send(Operation.RemoveAll)
        }
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        val gameTime = metadataManager.totalRuntimeInMilliseconds.collectAsState(initial = 0L)
        val isKubrikoInitialized = isInitialized.collectAsState().value
        val layers = layerIndices.collectAsState().value
        Box(
            modifier = if (isKubrikoInitialized) kubrikoImpl.managers.fold(Modifier.clipToBounds()) { modifierToProcess, manager ->
                manager.processModifierInternal(modifierToProcess, null, gameTime)
            } else Modifier.clipToBounds(),
        ) {
            layers.forEach { layerIndex ->
                Layer(
                    gameTime = gameTime,
                    layerIndex = layerIndex,
                )
            }
        }
    }

    @Composable
    private fun Layer(
        gameTime: State<Long>,
        layerIndex: Int?,
    ) {
        val drawBuffer = remember { ArrayList<Visible>() }
        val overlayBuffer = remember { ArrayList<Overlay>() }
        Canvas(
            modifier = if (layerIndex == null) {
                Modifier.fillMaxSize().clipToBounds()
            } else {
                kubrikoImpl.managers.fold(Modifier.fillMaxSize().clipToBounds()) { modifierToProcess, manager ->
                    manager.processModifierInternal(modifierToProcess, layerIndex, gameTime)
                }
            },
            onDraw = {
                @Suppress("UNUSED_EXPRESSION") gameTime.value
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
                        drawBuffer.clear()
                        val visibles = visibleActorsWithinViewport.value
                        val visiblesSize = visibles.size
                        for (i in 0 until visiblesSize) {
                            val actor = visibles[i]
                            if (actor.isVisible && actor.layerIndex == layerIndex) {
                                drawBuffer.add(actor)
                            }
                        }
                        drawBuffer.sortWith(drawingOrderComparator)
                        for (i in 0 until drawBuffer.size) {
                            val visible = drawBuffer[i]
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
                overlayBuffer.clear()
                val overlays = overlayActors.value
                for (i in 0 until overlays.size) {
                    val overlay = overlays[i]
                    if (overlay.layerIndex == layerIndex) {
                        overlayBuffer.add(overlay)
                    }
                }
                overlayBuffer.sortWith(overlayDrawingOrderComparator)
                for (i in 0 until overlayBuffer.size) {
                    val overlay = overlayBuffer[i]
                    with(overlay) { drawToViewport() }
                }
            }
        )
    }

    private sealed class Operation {
        class Add(val actors: List<Actor>) : Operation()
        class Remove(val actors: List<Actor>) : Operation()
        data object RemoveAll : Operation()
    }
}