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

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class ActorCullerTest {

    private class Box(x: Float, y: Float) : Actor, Visible {
        override val body = BoxBody(
            initialPosition = SceneOffset(x.sceneUnit, y.sceneUnit),
            initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit),
            initialPivot = SceneOffset.Zero,
        )

        override fun DrawScope.draw() = Unit
    }

    @Test
    fun publishesExactlyTheActorsWhoseBoundsTouchTheViewport() {
        // The 1920 x 1080 viewport is centered on the origin at scale 1 with no edge buffer: x in [-960, 960],
        // y in [-540, 540].
        val (kubriko, _) = newTestKubriko()
        val inside = Box(0f, 0f)
        val touchingRight = Box(960f, 0f)
        val touchingLeft = Box(-970f, 0f)
        val touchingTop = Box(0f, -550f)
        val touchingBottom = Box(0f, 540f)
        val pastRight = Box(960.5f, 0f)
        val pastTop = Box(0f, -550.5f)
        val visibleActorsWithinViewport = MutableStateFlow<ImmutableList<Visible>>(persistentListOf())
        val culler = ActorCuller(
            viewportManager = kubriko.viewportManager,
            metadataManager = kubriko.metadataManager,
            farAwayActorSleepMargin = null,
            invisibleActorMinimumRefreshTimeInMillis = 100,
            shouldComposeLayers = false,
            dynamicActors = MutableStateFlow<ImmutableList<Dynamic>>(persistentListOf()),
            visibleActors = MutableStateFlow(
                listOf<Visible>(inside, touchingRight, touchingLeft, touchingTop, touchingBottom, pastRight, pastTop)
                    .toImmutableList(),
            ),
            overlayActors = MutableStateFlow<ImmutableList<Overlay>>(persistentListOf()),
            _visibleActorsWithinViewport = visibleActorsWithinViewport,
            _activeDynamicActors = MutableStateFlow(persistentListOf()),
        )
        culler.refreshAfterUpdate(shouldPutFarAwayActorsToSleep = false, didCullDynamicActorsBeforeUpdate = false)
        assertEquals(
            listOf<Visible>(inside, touchingRight, touchingLeft, touchingTop, touchingBottom),
            visibleActorsWithinViewport.value.toList(),
        )
        kubriko.dispose()
    }
}
