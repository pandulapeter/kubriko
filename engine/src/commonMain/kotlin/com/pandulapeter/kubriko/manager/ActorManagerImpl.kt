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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
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
import com.pandulapeter.kubriko.helpers.extensions.minus
import com.pandulapeter.kubriko.helpers.extensions.transformForViewport
import com.pandulapeter.kubriko.helpers.extensions.transformViewport
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    private val _visibleActorsWithinViewport = MutableStateFlow<ImmutableList<Visible>>(persistentListOf())
    override val visibleActorsWithinViewport = _visibleActorsWithinViewport.asStateFlow()
    private val _activeDynamicActors = MutableStateFlow<ImmutableList<Dynamic>>(persistentListOf())
    override val activeDynamicActors = _activeDynamicActors.asStateFlow()
    private lateinit var kubrikoImpl: KubrikoImpl
    private val operationChannel = Channel<Operation>(Channel.UNLIMITED)
    private val drawingOrderComparator = Comparator<Visible> { a, b ->
        // +0f normalizes -0.0f to +0.0f: Float.compareTo distinguishes them via bit patterns,
        // causing A > B and B > C but A == C when one value is -0.0f, which violates the
        // transitivity contract that TimSort enforces and triggers an IllegalArgumentException.
        (b.drawingOrder + 0f).compareTo(a.drawingOrder + 0f)
    }
    private val overlayDrawingOrderComparator = Comparator<Overlay> { a, b ->
        (b.overlayDrawingOrder + 0f).compareTo(a.overlayDrawingOrder + 0f)
    }
    private val layerIndices by autoInitializingLazy {
        _allActors
            .map { actors -> actors.filterIsInstance<LayerAware>().groupBy { it.layerIndex }.keys.sortedBy { it }.toImmutableList() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }
    private val dynamicActors by autoInitializingLazy {
        _allActors
            .map { actors -> actors.filterIsInstance<Dynamic>().toImmutableList() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }
    private val visibleActors by autoInitializingLazy {
        _allActors
            .map { actors -> actors.filterIsInstance<Visible>().toImmutableList() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }
    private val overlayActors by autoInitializingLazy {
        _allActors
            .map { actors -> actors.filterIsInstance<Overlay>().toImmutableList() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .asStateFlowOnMainThread(persistentListOf())
    }

    // Pre-grouped and pre-sorted draw caches — rebuilt in onUpdate() when inputs change, read in onDraw()
    private var sortedVisibleActorsByLayer: Map<Int?, List<Visible>> = emptyMap()
    private var sortedOverlayActorsByLayer: Map<Int?, List<Overlay>> = emptyMap()

    // Reusable scratch buffers for visibility / active culling. Only ever touched on the tick thread
    // inside onUpdate(), never published anywhere, so they add no cross-thread sharing and let us cull
    // without allocating an intermediate list every frame.
    private val visibleScratch = ArrayList<Visible>()
    private val dynamicScratch = ArrayList<Dynamic>()

    // Mirror of the published active Dynamic list for the update loop: iterating the persistent list
    // directly would allocate a trie iterator every frame, while the ArrayList mirror is indexable in
    // O(1). Refilled only when the published list reference changes; tick-thread-private.
    private val activeDynamicMirror = ArrayList<Dynamic>()
    private var lastMirroredActiveDynamicActors: ImmutableList<Dynamic>? = null

    // Draw-cache reuse snapshots: the published list reference identifies the culled set, and the
    // primitive arrays capture each actor's drawingOrder/layerIndex at the last rebuild. When all
    // three are unchanged, the previously published sortedVisibleActorsByLayer is still correct and
    // the per-frame HashMap + ArrayList + sort rebuild can be skipped entirely (typical for static
    // scenes, menus, and paused games). Tick-thread-private.
    private var drawCacheActors: ImmutableList<Visible>? = null
    private var drawCacheDrawingOrders = FloatArray(0)
    private var drawCacheLayerIndices = LongArray(0)

    // Visibility cache invalidation tracking
    private var lastVisibleActors: ImmutableList<Visible>? = null
    private var lastVisibleRefreshTime = -1L
    private var lastViewportSizeForVisible: Size? = null

    // Dynamic actor cache invalidation tracking
    private var lastDynamicActors: ImmutableList<Dynamic>? = null
    private var lastDynamicRefreshTime = -1L
    private var lastViewportSizeForDynamic: Size? = null

    // Overlay cache invalidation tracking
    private var lastOverlayActors: ImmutableList<Overlay>? = null

    private fun updateVisibleActorsWithinViewport(viewportCenter: SceneOffset, scaleFactor: Scale) {
        val actors = visibleActors.value
        lastVisibleActors = actors
        val viewportSize = viewportManager.size.value
        lastViewportSizeForVisible = viewportSize
        lastVisibleRefreshTime = metadataManager.totalRuntimeInMilliseconds.value

        val halfScaledWidth = viewportSize.width / (scaleFactor.horizontal * 2f)
        val halfScaledHeight = viewportSize.height / (scaleFactor.vertical * 2f)
        val edgeBuffer = viewportManager.viewportEdgeBuffer.raw

        val leftBound = viewportCenter.x.raw - halfScaledWidth - edgeBuffer
        val topBound = viewportCenter.y.raw - halfScaledHeight - edgeBuffer
        val rightBound = viewportCenter.x.raw + halfScaledWidth + edgeBuffer
        val bottomBound = viewportCenter.y.raw + halfScaledHeight + edgeBuffer

        // Cull into the reusable scratch buffer instead of List.filter, which would allocate a fresh
        // ArrayList every frame (the predicate already inlines, so only the result list was the cost).
        // Iterate the source via its iterator: `actors` is a persistent vector whose indexed get() is
        // a trie walk, so a for-each is cheaper than indexing it per element.
        visibleScratch.clear()
        for (actor in actors) {
            if (actor.isAlwaysVisible) {
                visibleScratch.add(actor)
                continue
            }
            val aabb = actor.body.axisAlignedBoundingBox
            if (aabb.left.raw <= rightBound &&
                aabb.top.raw <= bottomBound &&
                aabb.right.raw >= leftBound &&
                aabb.bottom.raw >= topBound
            ) {
                visibleScratch.add(actor)
            }
        }
        // Only publish a fresh ImmutableList when the visible set actually changed. The StateFlow
        // already dedupes equal values, so consumers observe identical emissions and identical
        // .value contents — we just skip allocating the list in the common unchanged case.
        if (!_visibleActorsWithinViewport.value.contentEquals(visibleScratch)) {
            _visibleActorsWithinViewport.value = visibleScratch.toImmutableList()
        }

        // Pre-group by layer and pre-sort by drawingOrder so the draw loop is a plain indexed walk.
        // A brand-new map is published on every rebuild and never mutated afterwards, so the render
        // thread can read it lock-free even when a background TickSource drives onUpdate() off the
        // main thread. Built from the scratch ArrayList, whose indexed access is genuinely O(1).
        //
        // The rebuild is skipped when the culled set (identified by the published list reference)
        // and every actor's drawingOrder/layerIndex are unchanged since the last rebuild — the old
        // map is still correct, so a static scene pays one read-only scan instead of an
        // allocation + sort every frame. drawingOrder can change per frame (e.g. Y-sorted depth),
        // which the snapshot comparison catches.
        val publishedList = _visibleActorsWithinViewport.value
        val count = visibleScratch.size
        var canReuseDrawCache = publishedList === drawCacheActors
        if (canReuseDrawCache) {
            for (i in 0 until count) {
                val actor = visibleScratch[i]
                if (drawCacheDrawingOrders[i] != actor.drawingOrder ||
                    drawCacheLayerIndices[i] != actor.layerIndex.encodeLayerIndex()
                ) {
                    canReuseDrawCache = false
                    break
                }
            }
        }
        if (!canReuseDrawCache) {
            if (drawCacheDrawingOrders.size < count) {
                val newCapacity = maxOf(count, drawCacheDrawingOrders.size * 2)
                drawCacheDrawingOrders = FloatArray(newCapacity)
                drawCacheLayerIndices = LongArray(newCapacity)
            }
            sortedVisibleActorsByLayer = if (visibleScratch.isEmpty()) {
                emptyMap()
            } else {
                val grouped = HashMap<Int?, ArrayList<Visible>>()
                for (i in visibleScratch.indices) {
                    val actor = visibleScratch[i]
                    drawCacheDrawingOrders[i] = actor.drawingOrder
                    drawCacheLayerIndices[i] = actor.layerIndex.encodeLayerIndex()
                    grouped.getOrPut(actor.layerIndex) { ArrayList() }.add(actor)
                }
                for (list in grouped.values) {
                    list.sortWith(drawingOrderComparator)
                }
                grouped
            }
            drawCacheActors = publishedList
        }
    }

    // Long-encoded layerIndex snapshot value; Long.MIN_VALUE marks null (no Int maps to it).
    private fun Int?.encodeLayerIndex() = this?.toLong() ?: Long.MIN_VALUE

    private fun updateActiveDynamicActors(viewportCenter: SceneOffset, scaleFactor: Scale) {
        val actors = dynamicActors.value
        lastDynamicActors = actors
        val viewportSize = viewportManager.size.value
        lastViewportSizeForDynamic = viewportSize
        lastDynamicRefreshTime = metadataManager.activeRuntimeInMilliseconds.value

        val edgeBuffer = minOf(viewportSize.width / scaleFactor.horizontal, viewportSize.height / scaleFactor.vertical) / 2f
        val halfScaledWidth = viewportSize.width / (scaleFactor.horizontal * 2f)
        val halfScaledHeight = viewportSize.height / (scaleFactor.vertical * 2f)

        val leftBound = viewportCenter.x.raw - halfScaledWidth - edgeBuffer
        val topBound = viewportCenter.y.raw - halfScaledHeight - edgeBuffer
        val rightBound = viewportCenter.x.raw + halfScaledWidth + edgeBuffer
        val bottomBound = viewportCenter.y.raw + halfScaledHeight + edgeBuffer

        // Cull into the reusable scratch buffer, then only publish a fresh ImmutableList when the
        // active set changed (see updateVisibleActorsWithinViewport for the rationale).
        dynamicScratch.clear()
        for (actor in actors) {
            val isActive = if (!actor.isAlwaysActive && actor is Positionable) {
                val aabb = actor.body.axisAlignedBoundingBox
                aabb.left.raw <= rightBound &&
                        aabb.top.raw <= bottomBound &&
                        aabb.right.raw >= leftBound &&
                        aabb.bottom.raw >= topBound
            } else {
                true
            }
            if (isActive) {
                dynamicScratch.add(actor)
            }
        }
        if (!_activeDynamicActors.value.contentEquals(dynamicScratch)) {
            _activeDynamicActors.value = dynamicScratch.toImmutableList()
        }
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
            val currentActiveDynamicActors = activeDynamicActors.value
            if (currentActiveDynamicActors !== lastMirroredActiveDynamicActors) {
                lastMirroredActiveDynamicActors = currentActiveDynamicActors
                activeDynamicMirror.clear()
                activeDynamicMirror.addAll(currentActiveDynamicActors)
            }
            for (i in activeDynamicMirror.indices) {
                activeDynamicMirror[i].update(deltaTimeInMilliseconds)
            }
        }

        val viewportSize = viewportManager.size.value
        // Read camera and scale once per tick; both update functions share the same snapshot
        val cameraPosition = viewportManager.cameraPosition.value
        val scaleFactor = viewportManager.scaleFactor.value

        // Rebuild overlay draw cache only when the overlay list reference changes
        val currentOverlays = overlayActors.value
        if (currentOverlays !== lastOverlayActors) {
            lastOverlayActors = currentOverlays
            sortedOverlayActorsByLayer = if (currentOverlays.isEmpty()) {
                emptyMap()
            } else {
                val grouped = HashMap<Int?, ArrayList<Overlay>>()
                for (overlay in currentOverlays) {
                    grouped.getOrPut(overlay.layerIndex) { ArrayList() }.add(overlay)
                }
                // Sort each layer's list in place; mapValues would only allocate a second identical map.
                for (list in grouped.values) {
                    list.sortWith(overlayDrawingOrderComparator)
                }
                grouped
            }
        }

        // Rebuild visible actor draw cache when actors, viewport dimensions, or the throttle interval elapses
        val totalTime = metadataManager.totalRuntimeInMilliseconds.value
        val visibleActorsList = visibleActors.value
        if (!viewportSize.isEmpty()) {
            if (visibleActorsList !== lastVisibleActors
                || viewportSize != lastViewportSizeForVisible
                || (totalTime - lastVisibleRefreshTime) >= invisibleActorMinimumRefreshTimeInMillis
            ) {
                updateVisibleActorsWithinViewport(cameraPosition, scaleFactor)
            }
        }

        // Rebuild active dynamic actor list when actors, viewport dimensions, or the throttle interval elapses
        if (shouldPutFarAwayActorsToSleep) {
            val activeTime = metadataManager.activeRuntimeInMilliseconds.value
            val dynamicActorsList = dynamicActors.value
            if (!viewportSize.isEmpty()) {
                if (dynamicActorsList !== lastDynamicActors
                    || viewportSize != lastViewportSizeForDynamic
                    || (activeTime - lastDynamicRefreshTime) >= invisibleActorMinimumRefreshTimeInMillis
                ) {
                    updateActiveDynamicActors(cameraPosition, scaleFactor)
                }
            }
        } else if (_activeDynamicActors.value !== dynamicActors.value) {
            _activeDynamicActors.value = dynamicActors.value
        }
    }

    // Reference-equality, allocation-free comparison of a published list against a freshly culled
    // scratch buffer. Actors don't override equals, so identity comparison is the correct notion of
    // "same set in the same order" and lets us detect when re-publishing the StateFlow is unnecessary.
    // The receiver is the published persistent list (iterated, since its indexed get() is a trie walk);
    // `other` is the scratch ArrayList (indexed, genuinely O(1)).
    private fun <T> List<T>.contentEquals(other: List<T>): Boolean {
        if (size != other.size) return false
        var i = 0
        for (element in this) {
            if (element !== other[i]) return false
            i++
        }
        return true
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
                        val visibles = sortedVisibleActorsByLayer[layerIndex]
                        if (!visibles.isNullOrEmpty()) {
                            val canvas = drawContext.canvas
                            val transform = drawContext.transform
                            for (i in visibles.indices) {
                                val visible = visibles[i]
                                if (visible.isVisible) {
                                    canvas.save()
                                    visible.body.transformForViewport(transform)
                                    with(visible) {
                                        if (shouldClip) {
                                            transform.clipRect(
                                                left = 0f,
                                                top = 0f,
                                                right = body.size.width.raw,
                                                bottom = body.size.height.raw,
                                            )
                                        }
                                        draw()
                                    }
                                    canvas.restore()
                                }
                            }
                        }
                    }
                )
                val overlays = sortedOverlayActorsByLayer[layerIndex]
                if (!overlays.isNullOrEmpty()) {
                    for (i in overlays.indices) {
                        with(overlays[i]) { drawToViewport() }
                    }
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
