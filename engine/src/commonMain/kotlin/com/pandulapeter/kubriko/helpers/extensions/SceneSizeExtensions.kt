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

import androidx.compose.ui.util.lerp
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

val SceneSize.bottomRight get() = SceneOffset(width, height)

val SceneSize.minDimension get() = raw.minDimension.sceneUnit

val SceneSize.maxDimension get() = raw.maxDimension.sceneUnit

fun lerp(
    start: SceneSize,
    stop: SceneSize,
    fraction: Float,
) = SceneSize(
    width = lerp(start.width.raw, stop.width.raw, fraction).sceneUnit,
    height = lerp(start.height.raw, stop.height.raw, fraction).sceneUnit,
)