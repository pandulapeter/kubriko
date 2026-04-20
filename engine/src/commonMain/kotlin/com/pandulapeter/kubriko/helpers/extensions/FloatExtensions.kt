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
 * Converts this float to an [AngleDegrees] value.
 */
val Float.deg: AngleDegrees
    get() = AngleDegrees(this)

/**
 * Converts this float to an [AngleRadians] value.
 */
val Float.rad: AngleRadians
    get() = AngleRadians(this)

/**
 * Converts this float to a [SceneUnit] value.
 */
val Float.sceneUnit: SceneUnit
    get() = SceneUnit(this)

/**
 * Multiplies this float by a [SceneUnit].
 */
operator fun Float.times(scale: SceneUnit): SceneUnit = (scale.raw * this).sceneUnit