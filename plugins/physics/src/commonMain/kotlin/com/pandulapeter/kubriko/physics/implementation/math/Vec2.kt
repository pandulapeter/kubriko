/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation.math

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 2D Vectors class
 *
 * @param x Sets x value.
 * @param y Sets y value.
 */
// TODO: Use SceneOffset instead
data class Vec2(var x: SceneUnit = SceneUnit.Zero, var y: SceneUnit = SceneUnit.Zero) {

    /**
     * Copy constructor.
     *
     * @param vector Vector to copy.
     */
    constructor(vector: Vec2) : this(vector.x, vector.y)

    /**
     * Constructs a normalised direction vector.
     *
     * @param direction Direction in radians.
     */
    constructor(direction: Float) : this(cos(direction).sceneUnit, sin(direction).sceneUnit)

    fun toSceneOffset() = SceneOffset(x, y)

    /**
     * Sets a vector to equal an x/y value and returns this.
     *
     * @param x x value.
     * @param y y value.
     * @return The current instance vector.
     */
    fun set(x: SceneUnit, y: SceneUnit): Vec2 {
        this.x = x
        this.y = y
        return this
    }

    /**
     * Sets a vector to another vector and returns this.
     *
     * @param v1 Vector to set x/y values to.
     * @return The current instance vector.
     */
    fun set(v1: Vec2): Vec2 {
        x = v1.x
        y = v1.y
        return this
    }

    /**
     * Copy method to return a new copy of the current instance vector.
     *
     * @return A new Vec2 object.
     */
    fun copy(): Vec2 {
        return Vec2(x, y)
    }

    /**
     * Negates the current instance vector and return this.
     *
     * @return Return the negative form of the instance vector.
     */
    operator fun unaryMinus(): Vec2 {
        x = -x
        y = -y
        return this
    }

    /**
     * Negates the current instance vector and return this.
     *
     * @return Returns a new negative vector of the current instance vector.
     */
    fun copyNegative(): Vec2 {
        return Vec2(-x, -y)
    }

    /**
     * Adds a vector to the current instance and return this.
     *
     * @param v Vector to add.
     * @return Returns the current instance vector.
     */
    fun add(v: Vec2): Vec2 {
        x += v.x
        y += v.y
        return this
    }

    /**
     * Adds a vector and the current instance vector together and returns a new vector of them added together.
     *
     * @param v Vector to add.
     * @return Returns a new Vec2 of the sum of the addition of the two vectors.
     */
    operator fun plus(v: Vec2): Vec2 {
        return Vec2(x + v.x, y + v.y)
    }

    /**
     * Generates a normal of a vector. Normal facing to the right clock wise 90 degrees.
     *
     * @return A normal of the current instance vector.
     */
    fun normal(): Vec2 {
        return Vec2(-y, x)
    }

    /**
     * Normalizes the current instance vector to length 1 and returns this.
     *
     * @return Returns the normalized version of the current instance vector.
     */
    fun normalize(): Vec2 {
        var d = sqrt(x.raw * x.raw + y.raw * y.raw)
        if (d == 0f) {
            d = 1f
        }
        x /= d
        y /= d
        return this
    }

    /**
     * Finds the normalised version of a vector and returns a new vector of it.
     *
     * @return A normalized vector of the current instance vector.
     */
    val normalized: Vec2
        get() {
            var d = sqrt(x.raw * x.raw + y.raw * y.raw)
            if (d == 0f) {
                d = 1f
            }
            return Vec2(x / d, y / d)
        }

    /**
     * Finds the distance between two vectors.
     *
     * @param v Vector to find distance from.
     * @return Returns distance from vector v to the current instance vector.
     */
    fun distance(v: Vec2): SceneUnit {
        val dx = (x - v.x).raw
        val dy = (y - v.y).raw
        return sqrt(dx * dx + dy * dy).sceneUnit
    }

    /**
     * Subtract a vector from the current instance vector.
     *
     * @param v1 Vector to subtract.
     * @return Returns a new Vec2 with the subtracted vector applied
     */
    operator fun minus(v1: Vec2): Vec2 {
        return Vec2(x - v1.x, y - v1.y)
    }

    /**
     * Finds cross product between two vectors.
     *
     * @param v1 Other vector to apply cross product to
     * @return double
     */
    fun cross(v1: Vec2): Float {
        return x.raw * v1.y.raw - y.raw * v1.x.raw
    }

    fun cross(a: Float): Vec2 {
        return normal().scalar(a)
    }

    fun scalar(a: Float): Vec2 {
        return Vec2(x * a, y * a)
    }

    fun scalar(a: SceneUnit): Vec2 {
        return Vec2(x * a, y * a)
    }

    /**
     * Finds dotproduct between two vectors.
     *
     * @param v1 Other vector to apply dotproduct to.
     * @return double
     */
    fun dot(v1: Vec2): SceneUnit {
        return v1.x * x + v1.y * y
    }

    /**
     * Gets the length of instance vector.
     *
     * @return double
     */
    fun length(): SceneUnit {
        return sqrt(x.raw * x.raw + y.raw * y.raw).sceneUnit
    }

    /**
     * Checks to see if a vector has valid values set for x and y.
     *
     * @return boolean value whether a vector is valid or not.
     */
    val isValid: Boolean
        get() = !x.raw.isNaN() && !x.raw.isInfinite() && !y.raw.isNaN() && !y.raw.isInfinite()

    override fun toString(): String {
        return "$x : $y"
    }
}