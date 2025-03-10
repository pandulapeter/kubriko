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

import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

internal fun DrawTransform.transformViewport(
    viewportCenter: SceneOffset,
    shiftedViewportOffset: SceneOffset,
    viewportScaleFactor: Scale,
) {
    translate(
        left = shiftedViewportOffset.x.raw,
        top = shiftedViewportOffset.y.raw,
    )
    scale(
        scaleX = viewportScaleFactor.horizontal,
        scaleY = viewportScaleFactor.vertical,
        pivot = viewportCenter.raw,
    )
}