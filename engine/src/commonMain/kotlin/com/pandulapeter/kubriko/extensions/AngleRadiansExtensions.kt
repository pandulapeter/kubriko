/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val AngleRadians.deg get() = (normalized * (180f / PI).toFloat()).deg

val AngleRadians.sin get() = sin(normalized)

val AngleRadians.cos get() = cos(normalized)