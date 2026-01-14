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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

fun Offset.toSceneOffset(viewportManager: ViewportManager): SceneOffset = toSceneOffset(
    viewportCenter = viewportManager.cameraPosition.value,
    viewportSize = viewportManager.size.value,
    viewportScaleFactor = viewportManager.scaleFactor.value,
)

fun Offset.toSceneOffset(
    viewportCenter: SceneOffset,
    viewportSize: Size,
    viewportScaleFactor: Scale,
): SceneOffset = viewportCenter + SceneOffset(
    x = (x - viewportSize.width / 2).sceneUnit,
    y = (y - viewportSize.height / 2).sceneUnit,
) / viewportScaleFactor

operator fun Offset.div(scale: Scale) = Offset(
    x = x / scale.horizontal,
    y = y / scale.vertical,
)

operator fun Offset.times(scale: Scale) = Offset(
    x = x * scale.horizontal,
    y = y * scale.vertical,
)

operator fun Offset.times(scale: SceneUnit) = SceneOffset(
    x = x * scale,
    y = y * scale,
)
