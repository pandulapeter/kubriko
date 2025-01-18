/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.extensions

import com.pandulapeter.kubriko.types.AngleDegrees
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit

val Float.deg: AngleDegrees
    get() = AngleDegrees(this)

val Float.rad: AngleRadians
    get() = AngleRadians(this)

val Float.sceneUnit: SceneUnit
    get() = SceneUnit(this)

operator fun Float.times(scale: SceneUnit): SceneUnit = (scale.raw * this).sceneUnit