/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.implementation

import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

data class RotationMatrix(
    var row1: SceneOffset = SceneOffset.Zero,
    var row2: SceneOffset = SceneOffset.Zero,
) {
    constructor(radians: AngleRadians) : this() {
        set(radians)
    }

    fun set(radians: AngleRadians) {
        val c = radians.cos.sceneUnit
        val s = radians.sin.sceneUnit
        row1 = SceneOffset(
            x = c,
            y = -s,
        )
        row2 = SceneOffset(
            x = s,
            y = c,
        )
    }

    fun set(m: RotationMatrix) {
        row1 = m.row1
        row2 = m.row2
    }

    fun transpose() = RotationMatrix(
        row1 = SceneOffset(
            x = row1.x,
            y = row2.x,
        ),
        row2 = SceneOffset(
            x = row1.y,
            y = row2.y,
        )
    )

    operator fun times(v: SceneOffset) = SceneOffset(
        x = row1.x * v.x + row1.y * v.y,
        y = row2.x * v.x + row2.y * v.y,
    )
}