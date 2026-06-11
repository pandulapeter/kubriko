/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility

import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

// TODO: Use the version from Kubriko
fun SceneOffset.rotateAround(center: SceneOffset, radians: AngleRadians): SceneOffset {
    val cosA = radians.cos
    val sinA = radians.sin
    val dx = x - center.x
    val dy = y - center.y
    return SceneOffset(
        x = center.x + dx * cosA - dy * sinA,
        y = center.y + dx * sinA + dy * cosA
    )
}
