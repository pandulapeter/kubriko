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
import androidx.compose.runtime.key
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
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
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
    private val farAwayActorSleepMargin: SceneUnit?,
    private val invisibleActorMinimumRefreshTimeInMillis: Long,
    private val shouldComposeLayers: Boolean,
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
    private var isProcessingStarted = false
    private val drawingOrderComparator = Comparator<Visible> { a, b ->
        // +0f normalizes -0.0f to +0.0f: Float.compareTo distinguishes them via bit patterns,
        // causing A > B and B > C but A == C when one value is -0.0f, which violates the
        // transitivity contract that TimSort enforces and triggers an IllegalArgumentException.
        (b.drawingOrder + 0f).compareTo(a.drawingOrder + 0f)
    }
    private val overlayDrawingOrderComparator = Comparator<Overlay> { a, b ->
        (b.overlayDrawingOrder + 0f).compareTo(a.overlayDrawingOrder + 0f)
    }
    // A headless instance never composes a layer or draws an overlay, so it doesn't follow either of these through
    // every change of its actor list.
    private val layerIndices by autoInitializingLazy {
        if (!shouldComposeLayers) MutableStateFlow<ImmutableList<Int?>>(persistentListOf()).asStateFlow() else _allActors
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
     * This and the two lists below are derived by the batch processor itself and published before [allActors] (so
     * before any removal callback), rather than through a main-thread hop that would keep feeding removed actors to
     * the tick loop for a while.
     */
    private val dynamicActors = MutableStateFlow<ImmutableList<Dynamic>>(persistentListOf())
    private val visibleActors = MutableStateFlow<ImmutableList<Visible>>(persistentListOf())
    private val overlayActors = MutableStateFlow<ImmutableList<Overlay>>(persistentListOf())

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

        // Everything below only feeds the draw loop, so a headless instance stops here.
        if (!shouldComposeLayers) return

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

        val edgeBuffer = farAwayActorSleepMargin?.raw
            ?: (minOf(viewportSize.width / scaleFactor.horizontal, viewportSize.height / scaleFactor.vertical) / 2f)
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
        add(initialActors)
    }

    /**
     * Applies every operation queued so far synchronously on the calling thread, round after round until the
     * callbacks enqueue nothing more, then starts the background batch processor. Runs once per instance, when the
     * Kubriko instance is first started, so that `onAdded` always sees every Manager initialized and the first tick
     * sees the initial scene.
     */
    internal fun startProcessingOperations() {
        if (isProcessingStarted) return
        isProcessingStarted = true
        while (true) {
            val firstOperation = operationChannel.tryReceive().getOrNull() ?: break
            processBatchStartingWith(firstOperation)
        }
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                processBatchStartingWith(operationChannel.receive())
            }
        }
    }

    private fun processBatchStartingWith(firstOperation: Operation) {
        try {
            val batch = mutableListOf(firstOperation)
            while (true) {
                val operation = operationChannel.tryReceive().getOrNull() ?: break
                batch.add(operation)
            }
            processBatch(batch)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(
                message = "Actor batch processing failed.",
                details = e.stackTraceToString(),
            )
        }
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        // Refreshed before the update loop, so that a batch applied since the previous tick is already reflected in
        // the actors updated by this one.
        var didCullDynamicActorsBeforeUpdate = false
        val currentDynamicActors = dynamicActors.value
        if (currentDynamicActors !== lastDynamicActors) {
            if (!shouldPutFarAwayActorsToSleep) {
                lastDynamicActors = currentDynamicActors
                _activeDynamicActors.value = currentDynamicActors
            } else if (!viewportManager.size.value.isEmpty()) {
                updateActiveDynamicActors(viewportManager.cameraPosition.value, viewportManager.currentScaleFactor())
                didCullDynamicActorsBeforeUpdate = true
            }
        }

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
        val scaleFactor = viewportManager.currentScaleFactor()

        // Rebuild overlay draw cache only when the overlay list reference changes; a headless
        // instance never draws, so it has no cache to keep current.
        val currentOverlays = overlayActors.value
        if (shouldComposeLayers && currentOverlays !== lastOverlayActors) {
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
        if (shouldPutFarAwayActorsToSleep && !didCullDynamicActorsBeforeUpdate) {
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
        val visited = HashSet<Actor>()
        val queue = ArrayDeque(initialActors)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            result.add(current)
            if (current is Group) {
                for (child in current.actors) {
                    if (child !in visited) {
                        queue.addLast(child)
                    }
                }
            }
        }
        return result
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun processBatch(batch: List<Operation>) {
        val publishedList = _allActors.value
        // One mutable working copy plus a membership index for the whole batch: rebuilding the full
        // actor list per operation made a batch of individual calls quadratic, and the membership
        // checks below turned bulk removal into an O(removals x actors) scan.
        val workingList = ArrayList<Actor>(publishedList.size)
        workingList.addAll(publishedList)
        val workingSet = HashSet<Actor>(workingList.size * 2)
        workingSet.addAll(workingList)
        var didChange = false
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
                    if (latestUniqueByClass.isNotEmpty()) {
                        val uniqueTypesToReplace = latestUniqueByClass.keys
                        val iterator = workingList.iterator()
                        while (iterator.hasNext()) {
                            val actor = iterator.next()
                            if (actor::class in uniqueTypesToReplace && actor !== latestUniqueByClass[actor::class]) {
                                iterator.remove()
                                workingSet.remove(actor)
                                newlyRemoved.add(actor)
                                didChange = true
                            }
                        }
                    }
                    for (a in newActors) {
                        if (workingSet.add(a)) {
                            workingList.add(a)
                            didChange = true
                            if (!newlyRemoved.remove(a)) {
                                newlyAdded.add(a)
                            }
                        }
                    }
                }

                is Operation.Remove -> {
                    val flattenedActors = flattenActors(op.actors).asReversed()
                    val validRemovals = flattenedActors.filter { it in workingSet }
                    if (validRemovals.isNotEmpty()) {
                        val removalSet = validRemovals.toHashSet()
                        workingList.removeAll(removalSet)
                        workingSet.removeAll(removalSet)
                        didChange = true
                        newlyRemoved.addAll(validRemovals)
                    }
                }

                is Operation.RemoveAll -> {
                    if (workingList.isNotEmpty()) {
                        newlyRemoved.addAll(workingList)
                        workingList.clear()
                        workingSet.clear()
                        didChange = true
                    }
                }
            }
        }
        var firstFailure: Exception? = null
        for (actor in newlyAdded) {
            firstFailure = runActorCallback(actor, "onAdded", firstFailure) { actor.onAdded(kubrikoImpl) }
        }
        if (didChange) {
            publishDerivedActorLists(workingList)
            _allActors.value = workingList.toImmutableList()
        }
        for (actor in newlyRemoved) {
            if (actor is Disposable) {
                firstFailure = runActorCallback(actor, "dispose", firstFailure) { actor.dispose() }
            }
            firstFailure = runActorCallback(actor, "onRemoved", firstFailure) { actor.onRemoved() }
        }
        val failure = firstFailure
        if (failure != null) {
            scope.launch { throw failure }
        }
    }

    private fun publishDerivedActorLists(actors: List<Actor>) {
        val dynamics = ArrayList<Dynamic>()
        val visibles = ArrayList<Visible>()
        val overlays = ArrayList<Overlay>()
        for (actor in actors) {
            if (actor is Dynamic) dynamics.add(actor)
            if (actor is Visible) visibles.add(actor)
            if (shouldComposeLayers && actor is Overlay) overlays.add(actor)
        }
        if (!dynamicActors.value.contentEquals(dynamics)) dynamicActors.value = dynamics.toImmutableList()
        if (!visibleActors.value.contentEquals(visibles)) visibleActors.value = visibles.toImmutableList()
        if (!overlayActors.value.contentEquals(overlays)) overlayActors.value = overlays.toImmutableList()
    }

    /**
     * Runs one actor callback in isolation, so that a throwing actor never keeps the rest of the batch from being
     * applied. Returns the first failure of the batch: [firstFailure] if there already was one, otherwise the
     * exception thrown by [callback] (if any).
     */
    private inline fun runActorCallback(
        actor: Actor,
        callbackName: String,
        firstFailure: Exception?,
        callback: () -> Unit,
    ): Exception? = try {
        callback()
        firstFailure
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log(
            message = "Actor callback failed: $callbackName of $actor.",
            details = e.stackTraceToString(),
        )
        firstFailure ?: e
    }

    // Enqueued on the caller's own thread rather than from a coroutine, which is what makes operations
    // reach the loop above in the order they were issued: a coroutine each leaves that order to the
    // dispatcher, and a removal arriving before the addition it undoes finds nothing to remove. The
    // unbounded channel is what allows it - enqueueing never suspends. Only the ordering is the
    // caller's; the processing stays on the loop.
    override fun add(vararg actors: Actor) {
        if (actors.isEmpty()) return
        operationChannel.trySend(Operation.Add(actors.toList()))
    }

    override fun add(actors: Collection<Actor>) {
        if (actors.isEmpty()) return
        operationChannel.trySend(Operation.Add(actors.toList()))
    }

    override fun remove(vararg actors: Actor) {
        if (actors.isEmpty()) return
        operationChannel.trySend(Operation.Remove(actors.toList()))
    }

    override fun remove(actors: Collection<Actor>) {
        if (actors.isEmpty()) return
        operationChannel.trySend(Operation.Remove(actors.toList()))
    }

    override fun removeAll() {
        operationChannel.trySend(Operation.RemoveAll)
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (!shouldComposeLayers) return
        val gameTime = metadataManager.gameTime
        val isKubrikoInitialized = isInitialized.collectAsState().value
        Box(
            modifier = if (isKubrikoInitialized) kubrikoImpl.managers.fold(Modifier.clipToBounds()) { modifierToProcess, manager ->
                manager.processModifierInternal(modifierToProcess, null, gameTime)
            } else Modifier.clipToBounds(),
        ) {
            Layers(gameTime)
        }
    }

    /**
     * A scope of its own, so that a layer coming or going recomposes only the layers rather than rebuilding the
     * container's whole modifier chain (restarting its pointer handlers among others), and keyed by index, so that
     * one appearing in front of the others doesn't hand every later layer's node to a different layer.
     */
    @Composable
    private fun Layers(gameTime: State<Long>) {
        val layers = layerIndices.collectAsState().value
        layers.forEach { layerIndex ->
            key(layerIndex) {
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
                val visibles = sortedVisibleActorsByLayer[layerIndex]
                if (!visibles.isNullOrEmpty()) {
                    val viewportCenter = viewportManager.cameraPosition.value
                    val viewportSize = viewportManager.size.value
                    val scaleFactor = viewportManager.currentScaleFactor()
                    withTransform(
                        transformBlock = {
                            transformViewport(
                                viewportCenter = viewportCenter,
                                shiftedViewportOffset = (viewportSize / 2f) - viewportCenter,
                                viewportScaleFactor = scaleFactor,
                            )
                        },
                        drawBlock = {
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
                        },
                    )
                }
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
