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

import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun SceneUnit.clamp(
    min: SceneUnit? = null,
    max: SceneUnit? = null,
) = max(min?.raw ?: raw, min(max?.raw ?: raw, raw)).sceneUnit

val SceneUnit.abs get() = abs(raw).sceneUnit

fun abs(sceneUnit: SceneUnit) = sceneUnit.abs

fun min(a: SceneUnit, b: SceneUnit) = min(a.raw, b.raw).sceneUnit

fun max(a: SceneUnit, b: SceneUnit) = max(a.raw, b.raw).sceneUnit