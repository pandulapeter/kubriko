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

import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class ActorCuller(
    private val viewportManager: ViewportManagerImpl,
    private val metadataManager: MetadataManagerImpl,
    private val farAwayActorSleepMargin: SceneUnit?,
    private val invisibleActorMinimumRefreshTimeInMillis: Long,
    private val shouldComposeLayers: Boolean,
    private val dynamicActors: StateFlow<ImmutableList<Dynamic>>,
    private val visibleActors: StateFlow<ImmutableList<Visible>>,
    private val overlayActors: StateFlow<ImmutableList<Overlay>>,
    private val _visibleActorsWithinViewport: MutableStateFlow<ImmutableList<Visible>>,
    private val _activeDynamicActors: MutableStateFlow<ImmutableList<Dynamic>>,
) {
    private val drawingOrderComparator = Comparator<Visible> { a, b ->
        // +0f normalizes -0.0f to +0.0f: Float.compareTo distinguishes them via bit patterns,
        // causing A > B and B > C but A == C when one value is -0.0f, which violates the
        // transitivity contract that TimSort enforces and triggers an IllegalArgumentException.
        (b.drawingOrder + 0f).compareTo(a.drawingOrder + 0f)
    }
    private val overlayDrawingOrderComparator = Comparator<Overlay> { a, b ->
        (b.overlayDrawingOrder + 0f).compareTo(a.overlayDrawingOrder + 0f)
    }

    /**
     * This and [sortedOverlayActorsByLayer] are the draw caches, grouped by layer and sorted by drawing order: rebuilt
     * in [refreshAfterUpdate] when their inputs change, read by each layer's Canvas.
     */
    var sortedVisibleActorsByLayer: Map<Int?, List<Visible>> = emptyMap()
        private set
    var sortedOverlayActorsByLayer: Map<Int?, List<Overlay>> = emptyMap()
        private set

    /**
     * This and [dynamicScratch] are reusable cull buffers. Only ever touched on the tick thread inside `onUpdate` and
     * never published, so they add no cross-thread sharing and let culling run without allocating an intermediate list.
     */
    private val visibleScratch = ArrayList<Visible>()
    private val dynamicScratch = ArrayList<Dynamic>()

    /**
     * This and the two arrays below are the draw-cache reuse snapshots: the published list reference identifies the
     * culled set, and the primitive arrays capture each actor's drawingOrder/layerIndex at the last rebuild. When all
     * three are unchanged, the previously published sortedVisibleActorsByLayer is still correct and the per-frame
     * HashMap + ArrayList + sort rebuild can be skipped entirely (typical for static scenes, menus, and paused games).
     * Tick-thread-private.
     */
    private var drawCacheActors: ImmutableList<Visible>? = null
    private var drawCacheDrawingOrders = FloatArray(0)
    private var drawCacheLayerIndices = LongArray(0)

    /** This and the two below record when, and against which inputs, the visible set was last culled. */
    private var lastVisibleActors: ImmutableList<Visible>? = null
    private var lastVisibleRefreshTime = -1L
    private var lastViewportSizeForVisible: Size? = null

    /** This and the two below record when, and against which inputs, the active Dynamic set was last culled. */
    private var lastDynamicActors: ImmutableList<Dynamic>? = null
    private var lastDynamicRefreshTime = -1L
    private var lastViewportSizeForDynamic: Size? = null

    /** The overlay list [sortedOverlayActorsByLayer] was last built from. */
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

        // Cull into the reusable scratch buffer rather than a filtered copy. Iterate the source via its
        // iterator: `actors` is a persistent vector whose indexed get() is a trie walk, so a for-each is
        // cheaper than indexing it.
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

    /** Long-encoded layerIndex snapshot value; Long.MIN_VALUE marks null (no Int maps to it). */
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

    /**
     * Refreshes the active Dynamic set before the update loop when the Dynamic list changed since the last tick, and
     * returns whether that was a cull (which makes [refreshAfterUpdate] skip its own).
     */
    fun refreshActiveDynamicActorsBeforeUpdate(shouldPutFarAwayActorsToSleep: Boolean): Boolean {
        var didCullDynamicActorsBeforeUpdate = false
        val currentDynamicActors = dynamicActors.value
        if (currentDynamicActors !== lastDynamicActors) {
            if (!shouldPutFarAwayActorsToSleep) {
                lastDynamicActors = currentDynamicActors
                _activeDynamicActors.value = currentDynamicActors
            } else if (!viewportManager.size.value.isEmpty()) {
                updateActiveDynamicActors(viewportManager.cameraPosition.value, viewportManager.currentScaleFactor())
                didCullDynamicActorsBeforeUpdate = true
            } else {
                // Without a measured viewport nothing is far away; the first measured size triggers a real cull.
                lastDynamicActors = currentDynamicActors
                lastViewportSizeForDynamic = null
                if (_activeDynamicActors.value !== currentDynamicActors) {
                    _activeDynamicActors.value = currentDynamicActors
                }
            }
        }
        return didCullDynamicActorsBeforeUpdate
    }

    fun refreshAfterUpdate(shouldPutFarAwayActorsToSleep: Boolean, didCullDynamicActorsBeforeUpdate: Boolean) {
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
}
