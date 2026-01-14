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
import kotlin.math.sin

val AngleRadians.deg get() = (raw * (180f / PI).toFloat()).deg

val AngleRadians.sin get() = sin(normalized)

val AngleRadians.cos get() = cos(normalized)

/**
 * Rotates this angle towards the [targetAngle] by a step not exceeding [maxDelta].
 *
 * If the difference between this angle and the [targetAngle] is less than [maxDelta],
 * the returned angle will be exactly [targetAngle]. Otherwise, the angle is rotated
 * by [maxDelta] in the direction of [targetAngle].
 *
 * @param targetAngle The destination angle.
 * @param maxDelta The maximum amount to rotate by (should be non-negative).
 * @return The new angle after rotation.
 */
fun AngleRadians.rotateTowards(targetAngle: AngleRadians, maxDelta: AngleRadians): AngleRadians {
    val delta = targetAngle - this
    return if (delta == AngleRadians.Zero || abs(delta.raw) < 0.0000001f) {
        targetAngle
    } else if (delta.raw < 0) {
        if (-maxDelta > delta) {
            this - maxDelta
        } else {
            this + delta
        }
    } else {
        if (maxDelta < delta) {
            this + maxDelta
        } else {
            this + delta
        }
    }
}
