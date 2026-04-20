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
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Converts this long to an [AngleDegrees] value.
 */
val Long.deg: AngleDegrees
    get() = AngleDegrees(toFloat())

/**
 * Converts this long to an [AngleRadians] value.
 */
val Long.rad: AngleRadians
    get() = AngleRadians(toFloat())

/**
 * Converts this long to a [SceneUnit] value.
 */
val Long.sceneUnit: SceneUnit
    get() = SceneUnit(toFloat())

/**
 * Multiplies this long by a [SceneUnit].
 */
operator fun Long.times(scale: SceneUnit): SceneUnit = (scale.raw * this).sceneUnit