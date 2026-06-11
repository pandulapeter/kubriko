/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.generic

import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

// TODO: Reduce new instance creation
data class Vec3(
    var x: SceneUnit,
    var y: SceneUnit,
    var z: SceneUnit,
) {
    val positionXY
        get() = SceneOffset(
            x = x,
            y = y,
        )

    fun rotateX(angleRadians: AngleRadians) = copy(
        y = y * angleRadians.cos - z * angleRadians.sin,
        z = y * angleRadians.sin + z * angleRadians.cos,
    )

    fun rotateY(angleRadians: AngleRadians) = copy(
        x = x * angleRadians.cos + z * angleRadians.sin,
        z = -x * angleRadians.sin + z * angleRadians.cos,
    )

    fun rotateZ(angleRadians: AngleRadians) = copy(
        x = x * angleRadians.cos - y * angleRadians.sin,
        y = x * angleRadians.sin + y * angleRadians.cos,
    )

    fun toOffsetIso(tileWidth: SceneUnit, tileHeight: SceneUnit, depthEffect: Float) = SceneOffset(
        (x - y) * 0.5f * tileWidth,
        (x + y) * 0.5f * tileHeight - (z / depthEffect) * tileHeight
    )

    operator fun plus(other: Vec3) = copy(
        x = x + other.x,
        y = y + other.y,
        z = z + other.z,
    )

    operator fun minus(other: Vec3) = copy(
        x = x - other.x,
        y = y - other.y,
        z = z - other.z,
    )

    companion object {
        val Zero
            get() = Vec3(
                x = SceneUnit.Zero,
                y = SceneUnit.Zero,
                z = SceneUnit.Zero,
            )
    }
}