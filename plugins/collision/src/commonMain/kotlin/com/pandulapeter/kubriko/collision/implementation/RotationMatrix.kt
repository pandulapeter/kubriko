/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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

/**
 * A 2×2 rotation matrix stored as two rows.
 */
data class RotationMatrix(
    /** The first row of the matrix: (cos, -sin) for a rotation. */
    var row1: SceneOffset = SceneOffset.Zero,
    /** The second row of the matrix: (sin, cos) for a rotation. */
    var row2: SceneOffset = SceneOffset.Zero,
) {
    /**
     * Creates the matrix of a rotation by [radians].
     *
     * @param radians The angle of the rotation.
     */
    constructor(radians: AngleRadians) : this() {
        set(radians)
    }

    /**
     * Overwrites this matrix with the rotation by [radians].
     *
     * @param radians The angle of the rotation.
     */
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

    /**
     * Overwrites this matrix with the rows of [m].
     *
     * @param m The matrix to copy.
     */
    fun set(m: RotationMatrix) {
        row1 = m.row1
        row2 = m.row2
    }

    /**
     * Returns the transpose of this matrix (the inverse rotation) as a new instance. Use [transposeInto] to avoid
     * the allocation.
     */
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

    /**
     * Writes the transpose of this matrix (the inverse rotation) into [dest] without allocating a new matrix.
     *
     * @param dest The matrix to overwrite. It must be a different instance than this one: [dest]'s first row is
     * written before this matrix's first row is fully read, so transposing in place gives a wrong result.
     */
    fun transposeInto(dest: RotationMatrix) {
        dest.row1 = SceneOffset(x = row1.x, y = row2.x)
        dest.row2 = SceneOffset(x = row1.y, y = row2.y)
    }

    /**
     * Returns [v] rotated by this matrix. Neither this matrix nor [v] is changed.
     *
     * @param v The vector to rotate.
     */
    operator fun times(v: SceneOffset) = SceneOffset(
        x = row1.x * v.x + row1.y * v.y,
        y = row2.x * v.x + row2.y * v.y,
    )
}