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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

operator fun Size.minus(offset: Offset) = Offset(
    x = width - offset.x,
    y = height - offset.y,
)

operator fun Size.minus(offset: SceneOffset) = SceneOffset(
    x = width.sceneUnit - offset.x,
    y = height.sceneUnit - offset.y,
)

operator fun Size.div(scale: Scale) = Size(
    width = width / scale.horizontal,
    height = height / scale.vertical,
)


fun Size.toSceneSize(viewportManager: ViewportManager): SceneSize = toSceneSize(
    viewportSize = viewportManager.size.value,
    viewportScaleFactor = viewportManager.scaleFactor.value,
)

fun Size.toSceneSize(
    viewportSize: Size,
    viewportScaleFactor: Scale,
): SceneSize = SceneSize(
    width = (width - viewportSize.width / 2).sceneUnit,
    height = (height - viewportSize.height / 2).sceneUnit,
) / viewportScaleFactor