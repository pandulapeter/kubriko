package com.pandulapeter.kubriko.physics.implementation.physics.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 2D Vectors class
 *
 * @param x Sets x value.
 * @param y Sets y value.
 */
// TODO: Should use SceneOffset instead
data class Vec2(var x: Float = 0f, var y: Float = 0f) {

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
    constructor(direction: Float) : this(cos(direction).toFloat(), sin(direction).toFloat())

    /**
     * Sets a vector to equal an x/y value and returns this.
     *
     * @param x x value.
     * @param y y value.
     * @return The current instance vector.
     */
    operator fun set(x: Float, y: Float): Vec2 {
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
        var d = sqrt(x * x + y * y)
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
            var d = sqrt(x * x + y * y)
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
    fun distance(v: Vec2): Float {
        val dx = x - v.x
        val dy = y - v.y
        return sqrt(dx * dx + dy * dy)
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
        return x * v1.y - y * v1.x
    }

    fun cross(a: Float): Vec2 {
        return normal().scalar(a)
    }

    fun scalar(a: Float): Vec2 {
        return Vec2(x * a, y * a)
    }

    /**
     * Finds dotproduct between two vectors.
     *
     * @param v1 Other vector to apply dotproduct to.
     * @return double
     */
    fun dot(v1: Vec2): Float {
        return v1.x * x + v1.y * y
    }

    /**
     * Gets the length of instance vector.
     *
     * @return double
     */
    fun length(): Float {
        return sqrt(x * x + y * y)
    }

    /**
     * Checks to see if a vector has valid values set for x and y.
     *
     * @return boolean value whether a vector is valid or not.
     */
    val isValid: Boolean
        get() = !x.isNaN() && !x.isInfinite() && !y.isNaN() && !y.isInfinite()

    /**
     * Checks to see if a vector is set to (0,0).
     *
     * @return boolean value whether the vector is set to (0,0).
     */
    val isZero: Boolean
        get() = abs(x) == 0f && abs(y) == 0f

    override fun toString(): String {
        return "$x : $y"
    }

    companion object {
        /**
         * Static method for any cross product, same as
         * [.cross]
         *
         * @param s double.
         * @param a Vec2.
         * @return Cross product scalar result.
         */
        fun cross(a: Vec2, s: Float): Vec2 {
            return Vec2(s * a.y, -s * a.x)
        }

        /**
         * Finds the cross product of a scalar and a vector. Produces a scalar in 2D.
         *
         * @param s double.
         * @param a Vec2.
         * @return Cross product scalar result.
         */

        fun cross(s: Float, a: Vec2): Vec2 {
            return Vec2(-s * a.y, s * a.x)
        }

        /**
         * Generates an array of length n with zero initialised vectors.
         *
         * @param n Length of array.
         * @return A Vec2 array of zero initialised vectors.
         */

        fun createArray(n: Int): Array<Vec2?> {
            val array = arrayOfNulls<Vec2>(n)

            array.forEach {
                if (it != null) {
                    it.x = 0f
                    it.y = 0f
                }
            }
            return array
        }

        val ZERO = Vec2(0f, 0f)
        val DOWN = Vec2(0f, -1f)
        val UP = Vec2(0f, 1f)
        val LEFT = Vec2(-1f, 0f)
        val RIGHT = Vec2(1f, 0f)
    }
}