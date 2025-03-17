package com.pandulapeter.kubriko.collision.implementation

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// TODO: Use SceneOffset instead of this class
data class Vec2(
    var x: SceneUnit = SceneUnit.Zero,
    var y: SceneUnit = SceneUnit.Zero,
) {
    constructor(vector: Vec2) : this(vector.x, vector.y)

    constructor(direction: Float) : this(cos(direction).sceneUnit, sin(direction).sceneUnit)

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