/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/**
 * Converts this angle from radians to degrees.
 */
val AngleRadians.deg get() = (raw * (180f / PI).toFloat()).deg

/**
 * Returns the sine of this angle.
 */
val AngleRadians.sin get() = sin(normalized)

/**
 * Returns the cosine of this angle.
 */
val AngleRadians.cos get() = cos(normalized)

/**
 * Rotates this angle towards the [target] angle by a step not exceeding [maxDelta].
 *
 * @param target The destination angle.
 * @param maxDelta The maximum amount to rotate by (should be non-negative).
 * @return The new angle after rotation.
 */
fun AngleRadians.rotateTowards(
    target: AngleRadians,
    maxDelta: AngleRadians
): AngleRadians {
    val delta = shortestDeltaTo(target)
    if (abs(delta.raw) <= maxDelta.raw) {
        return target
    }
    return (this.raw + (maxDelta.raw * sign(delta.raw))).rad
}

/**
 * Calculates the shortest signed distance between this angle and the [target] angle.
 *
 * @param target The destination angle.
 * @return The shortest signed difference in radians.
 */
fun AngleRadians.shortestDeltaTo(target: AngleRadians): AngleRadians {
    val diff = (target.raw - this.raw) % AngleRadians.TwoPi.raw
    return when {
        diff > AngleRadians.Pi.raw -> diff - AngleRadians.TwoPi.raw
        diff <= -AngleRadians.Pi.raw -> diff + AngleRadians.TwoPi.raw
        else -> diff
    }.rad
}