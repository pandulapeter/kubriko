package com.pandulapeter.kubriko.physicsManager.implementation.math

import kotlin.math.cos
import kotlin.math.sin

class Mat2 {
    var row1 = Vec2()

    var row2 = Vec2()

    /**
     * Default constructor matrix [(0,0),(0,0)] by default.
     */
    constructor()

    /**
     * Constructs and sets the matrix up to be a rotation matrix that stores the angle specified in the matrix.
     * @param radians The desired angle of the rotation matrix
     */
    constructor(radians: Double) {
        this.set(radians)
    }

    /**
     * Sets the matrix up to be a rotation matrix that stores the angle specified in the matrix.
     * @param radians The desired angle of the rotation matrix
     */
    fun set(radians: Double) {
        val c = cos(radians)
        val s = sin(radians)
        row1.x = c
        row1.y = -s
        row2.x = s
        row2.y = c
    }

    /**
     * Sets current object matrix to be the same as the supplied parameters matrix.
     * @param m Matrix to set current object to
     */
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

    fun mul(v: Vec2?): Vec2 {
        val x = v!!.x
        val y = v.y
        v.x = row1.x * x + row1.y * y
        v.y = row2.x * x + row2.y * y
        return v
    }

    fun mul(v: Vec2?, out: Vec2): Vec2 {
        out.x = row1.x * v!!.x + row1.y * v.y
        out.y = row2.x * v.x + row2.y * v.y
        return out
    }

    override fun toString(): String {
        return """${row1.x} : ${row1.y}
${row2.x} : ${row2.y}"""
    }
}