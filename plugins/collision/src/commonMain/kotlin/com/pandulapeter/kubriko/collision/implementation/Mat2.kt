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

class Mat2 {
    var row1 = Vec2()
    var row2 = Vec2()

    constructor()

    constructor(radians: AngleRadians) {
        this.set(radians)
    }

    fun set(radians: AngleRadians) {
        val c = radians.cos.sceneUnit
        val s = radians.sin.sceneUnit
        row1.x = c
        row1.y = -s
        row2.x = s
        row2.y = c
    }

    fun set(m: Mat2) {
        row1.x = m.row1.x
        row1.y = m.row1.y
        row2.x = m.row2.x
        row2.y = m.row2.y
    }

    fun transpose(): Mat2 {
        val mat = Mat2()
        mat.row1.x = row1.x
        mat.row1.y = row2.x
        mat.row2.x = row1.y
        mat.row2.y = row2.y
        return mat
    }

    fun mul(v: Vec2) = Vec2(
        x = row1.x * v.x + row1.y * v.y,
        y = row2.x * v.x + row2.y * v.y,
    )

    fun mul(v: SceneOffset) = SceneOffset(
        x = row1.x * v.x + row1.y * v.y,
        y = row2.x * v.x + row2.y * v.y,
    )
}