/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testInput.implementation.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class InputTestManager : Manager(), KeyboardInputAware, PointerInputAware, Overlay, Unique {
    private val _activeKeys = MutableStateFlow(emptySet<Key>())
    val activeKeys = _activeKeys.asStateFlow()
    private var pointerOffset: Offset? = null
    private var isPointerBeingPressed = false

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(this)
    }

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = _activeKeys.update { activeKeys }

    override fun onPointerOffsetChanged(pointerId: PointerId?, screenOffset: Offset) {
        pointerOffset = screenOffset
    }

    override fun onPointerPressed(pointerId: PointerId, screenOffset: Offset) {
        isPointerBeingPressed = true
    }

    override fun onPointerReleased(pointerId: PointerId, screenOffset: Offset) {
        isPointerBeingPressed = false
    }

    override fun DrawScope.drawToViewport() {
        pointerOffset?.let { pointerOffset ->
            drawLine(
                color = Color.Black,
                start = Offset(pointerOffset.x, 0f),
                end = Offset(pointerOffset.x, size.height),
                strokeWidth = 4f,
            )
            drawLine(
                color = Color.White,
                start = Offset(pointerOffset.x, 0f),
                end = Offset(pointerOffset.x, size.height),
                strokeWidth = 2f,
            )
            drawLine(
                color = Color.Black,
                start = Offset(0f, pointerOffset.y),
                end = Offset(size.width, pointerOffset.y),
                strokeWidth = 4f,
            )
            drawLine(
                color = Color.White,
                start = Offset(0f, pointerOffset.y),
                end = Offset(size.width, pointerOffset.y),
                strokeWidth = 2f,
            )
            drawCircle(
                color = Color.White,
                radius = if (isPointerBeingPressed) 40f else 20f,
                center = pointerOffset,
            )
            drawCircle(
                color = Color.Black,
                radius = if (isPointerBeingPressed) 40f else 20f,
                center = pointerOffset,
                style = Stroke(),
            )
        }
    }
}