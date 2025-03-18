/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.sqrt

@Deprecated("Should be replaced with SceneOffset") // TODO: Use SceneOffset instead of this class
internal data class Vec2(
    var x: SceneUnit = SceneUnit.Companion.Zero,
    var y: SceneUnit = SceneUnit.Companion.Zero,
) {
    constructor(vector: Vec2) : this(vector.x, vector.y)

    constructor(direction: AngleRadians) : this(direction.cos.sceneUnit, direction.sin.sceneUnit)

    fun toSceneOffset() = SceneOffset(x, y)

    fun set(x: SceneUnit, y: SceneUnit): Vec2 {
        this.x = x
        this.y = y
        return this
    }

    fun set(v1: Vec2): Vec2 {
        x = v1.x
        y = v1.y
        return this
    }

    fun copy() = Vec2(x, y)

    operator fun unaryMinus(): Vec2 {
        x = -x
        y = -y
        return this
    }

    fun copyNegative() = Vec2(-x, -y)

    fun add(v: Vec2): Vec2 {
        x += v.x
        y += v.y
        return this
    }

    operator fun plus(v: Vec2) = Vec2(x + v.x, y + v.y)

    fun normal() = Vec2(-y, x)

    fun normalize(): Vec2 {
        var d = sqrt(x.raw * x.raw + y.raw * y.raw)
        if (d == 0f) {
            d = 1f
        }
        x /= d
        y /= d
        return this
    }

    val normalized: Vec2
        get() {
            var d = sqrt(x.raw * x.raw + y.raw * y.raw)
            if (d == 0f) {
                d = 1f
            }
            return Vec2(x / d, y / d)
        }

    fun distance(v: Vec2): SceneUnit {
        val dx = (x - v.x).raw
        val dy = (y - v.y).raw
        return sqrt(dx * dx + dy * dy).sceneUnit
    }

    operator fun minus(v1: Vec2) = Vec2(x - v1.x, y - v1.y)

    fun cross(v1: Vec2) = x.raw * v1.y.raw - y.raw * v1.x.raw

    fun cross(a: Float) = normal().scalar(a)

    fun scalar(a: Float) = Vec2(x * a, y * a)

    fun scalar(a: SceneUnit) = Vec2(x * a, y * a)

    fun dot(v1: Vec2) = v1.x * x + v1.y * y

    fun length() = sqrt(x.raw * x.raw + y.raw * y.raw).sceneUnit
}