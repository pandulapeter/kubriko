/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.implementation.windowState
import java.awt.Robot
import kotlin.math.roundToInt

private val robot by lazy { Robot() }

internal actual fun setPointerPosition(offset: Offset, densityMultiplier: Float) {
    val x = windowState.position.x.value + offset.x * densityMultiplier
    val y = windowState.position.y.value + offset.y * densityMultiplier
    robot.mouseMove(x.roundToInt(), y.roundToInt())
}