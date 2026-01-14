/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

internal class InputTestManager : Manager(), KeyboardInputAware, PointerInputAware, Overlay, Unique {
    private val _activeKeys = MutableStateFlow(emptySet<Key>())
    val activeKeys = _activeKeys.asStateFlow()
    private val pointerInputManager by manager<PointerInputManager>()

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(this)
    }

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = _activeKeys.update { activeKeys }

    private fun PointerId?.toColor() = this?.value?.let { value ->
        Color.hsv(abs((value * 47) % 360).toFloat(), 0.8f, 0.8f)
    } ?: Color.White

    override fun DrawScope.drawToViewport() {
        pointerInputManager.hoveringPointerPosition.value?.let { pointerOffset ->
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
                radius = 20f,
                center = pointerOffset,
            )
            drawCircle(
                color = Color.Black,
                radius = 20f,
                center = pointerOffset,
                style = Stroke(),
            )
        }
        pointerInputManager.pressedPointerPositions.value.forEach { (pointerId, pointerOffset) ->
            val color = pointerId.toColor()
            drawLine(
                color = Color.Black,
                start = Offset(pointerOffset.x, 0f),
                end = Offset(pointerOffset.x, size.height),
                strokeWidth = 4f,
            )
            drawLine(
                color = color,
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
                color = color,
                start = Offset(0f, pointerOffset.y),
                end = Offset(size.width, pointerOffset.y),
                strokeWidth = 2f,
            )
            drawCircle(
                color = color,
                radius = 40f,
                center = pointerOffset,
            )
            drawCircle(
                color = Color.Black,
                radius = 40f,
                center = pointerOffset,
                style = Stroke(),
            )
        }
    }
}