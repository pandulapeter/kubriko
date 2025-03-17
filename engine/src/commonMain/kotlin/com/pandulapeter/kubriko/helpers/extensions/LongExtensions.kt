/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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

val Long.deg: AngleDegrees
    get() = AngleDegrees(toFloat())

val Long.rad: AngleRadians
    get() = AngleRadians(toFloat())

val Long.sceneUnit: SceneUnit
    get() = SceneUnit(toFloat())

operator fun Long.times(scale: SceneUnit): SceneUnit = (scale.raw * this).sceneUnit