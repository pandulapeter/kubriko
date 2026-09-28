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
import com.pandulapeter.kubriko.manager.ViewportManagerImpl
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Converts this screen position (in pixels, relative to the viewport's top-left corner) to the scene position it shows,
 * using the camera position, viewport size and scale factor of [viewportManager].
 * To convert a pixel distance or direction instead, divide it by the scale factor: `SceneOffset(pixels / viewportManager.scaleFactor.value)`.
 *
 * @param viewportManager The [ViewportManager] used for conversion.
 */
fun Offset.toSceneOffset(viewportManager: ViewportManager): SceneOffset = toSceneOffset(
    viewportCenter = viewportManager.cameraPosition.value,
    viewportSize = viewportManager.size.value,
    viewportScaleFactor = (viewportManager as ViewportManagerImpl).currentScaleFactor(),
)

/**
 * Converts this screen position (in pixels, relative to the viewport's top-left corner) to the scene position it shows.
 *
 * @param viewportCenter The current center of the camera in the scene.
 * @param viewportSize The size of the viewport in screen pixels.
 * @param viewportScaleFactor The current scale factor of the viewport.
 */
fun Offset.toSceneOffset(
    viewportCenter: SceneOffset,
    viewportSize: Size,
    viewportScaleFactor: Scale,
): SceneOffset = viewportCenter + SceneOffset(
    x = (x - viewportSize.width / 2).sceneUnit,
    y = (y - viewportSize.height / 2).sceneUnit,
) / viewportScaleFactor

/**
 * Divides this [Offset] by a [Scale].
 */
operator fun Offset.div(scale: Scale) = Offset(
    x = x / scale.horizontal,
    y = y / scale.vertical,
)

/**
 * Multiplies this [Offset] by a [Scale].
 */
operator fun Offset.times(scale: Scale) = Offset(
    x = x * scale.horizontal,
    y = y * scale.vertical,
)

/**
 * Multiplies this [Offset] by a [SceneUnit].
 */
operator fun Offset.times(scale: SceneUnit) = SceneOffset(
    x = x * scale,
    y = y * scale,
)
