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

import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.newTestKubriko
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The draw order the layers read from [ActorCuller]: the documented painter's order of `layerIndex`, `drawingOrder`
 * and `overlayDrawingOrder` is only observable by drawing, which library tests cannot do.
 */
class ActorCullerTest {

    private val kubriko = newTestKubriko().first
    private val visibleActors = MutableStateFlow<ImmutableList<Visible>>(persistentListOf())
    private val overlayActors = MutableStateFlow<ImmutableList<Overlay>>(persistentListOf())
    private val culler = ActorCuller(
        viewportManager = kubriko.viewportManager,
        metadataManager = kubriko.metadataManager,
        farAwayActorSleepMargin = null,
        invisibleActorMinimumRefreshTimeInMillis = 0,
        shouldComposeLayers = true,
        dynamicActors = MutableStateFlow<ImmutableList<Dynamic>>(persistentListOf()),
        visibleActors = visibleActors,
        overlayActors = overlayActors,
        _visibleActorsWithinViewport = MutableStateFlow(persistentListOf()),
        _activeDynamicActors = MutableStateFlow(persistentListOf()),
    )

    private fun cull() = culler.refreshAfterUpdate(shouldPutFarAwayActorsToSleep = false, didCullDynamicActorsBeforeUpdate = false)

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun visibleActorsAreGroupedByLayer() {
        val background = TestVisible(0f, 0f, layerIndex = -1)
        val foreground = TestVisible(0f, 0f, layerIndex = 3)
        val unlayered = TestVisible(0f, 0f, layerIndex = null)
        visibleActors.value = listOf<Visible>(foreground, unlayered, background).toImmutableList()

        cull()

        assertEquals(setOf(-1, 3, null), culler.sortedVisibleActorsByLayer.keys)
        assertEquals(listOf<Visible>(background), culler.sortedVisibleActorsByLayer[-1])
        assertEquals(listOf<Visible>(foreground), culler.sortedVisibleActorsByLayer[3])
        assertEquals(listOf<Visible>(unlayered), culler.sortedVisibleActorsByLayer[null])
    }

    @Test
    fun lowerDrawingOrderIsDrawnLater() {
        val top = TestVisible(0f, 0f, drawingOrder = -5f)
        val middle = TestVisible(0f, 0f, drawingOrder = 0f)
        val bottom = TestVisible(0f, 0f, drawingOrder = 7.5f)
        visibleActors.value = listOf<Visible>(middle, top, bottom).toImmutableList()

        cull()

        assertEquals(listOf<Visible>(bottom, middle, top), culler.sortedVisibleActorsByLayer[0])
    }

    @Test
    fun culledActorsAreNotDrawn() {
        val inside = TestVisible(0f, 0f)
        val outside = TestVisible(5_000f, 0f)
        visibleActors.value = listOf<Visible>(inside, outside).toImmutableList()

        cull()

        assertEquals(listOf<Visible>(inside), culler.sortedVisibleActorsByLayer[0])
    }

    @Test
    fun drawingOrderChangeIsReflectedOnTheNextCull() {
        val first = TestVisible(0f, 0f, drawingOrder = 1f)
        val second = TestVisible(0f, 0f, drawingOrder = 2f)
        visibleActors.value = listOf<Visible>(first, second).toImmutableList()
        cull()

        first.drawingOrder = 3f
        cull()

        assertEquals(listOf<Visible>(first, second), culler.sortedVisibleActorsByLayer[0])
    }

    @Test
    fun layerIndexChangeIsReflectedOnTheNextCull() {
        val actor = TestVisible(0f, 0f, layerIndex = 0)
        visibleActors.value = listOf<Visible>(actor).toImmutableList()
        cull()

        actor.layerIndex = 2
        cull()

        assertEquals(setOf<Int?>(2), culler.sortedVisibleActorsByLayer.keys)
    }

    @Test
    fun overlaysAreGroupedByLayerAndLowerOverlayDrawingOrderIsDrawnLater() {
        val top = TestOverlay(overlayDrawingOrder = -1f)
        val bottom = TestOverlay(overlayDrawingOrder = 1f)
        val otherLayer = TestOverlay(layerIndex = 1)
        overlayActors.value = listOf<Overlay>(top, otherLayer, bottom).toImmutableList()

        cull()

        assertEquals(listOf<Overlay>(bottom, top), culler.sortedOverlayActorsByLayer[0])
        assertEquals(listOf<Overlay>(otherLayer), culler.sortedOverlayActorsByLayer[1])
    }
}
