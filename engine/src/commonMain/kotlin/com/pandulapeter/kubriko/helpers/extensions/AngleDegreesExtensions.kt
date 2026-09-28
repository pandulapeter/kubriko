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

import com.pandulapeter.kubriko.types.AngleDegrees
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Converts this angle from degrees to radians. The value is not wrapped; use [AngleDegrees.normalized] (or
 * `AngleRadians.normalized`) for that.
 */
val AngleDegrees.rad get() = (raw * (PI / 180f).toFloat()).rad


/**
 * Returns the sine of this angle.
 */
val AngleDegrees.sin get() = sin(rad.raw)

/**
 * Returns the cosine of this angle.
 */
val AngleDegrees.cos get() = cos(rad.raw)