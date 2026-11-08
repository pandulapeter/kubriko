/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.skiaCanvas

internal actual fun Canvas.pixelsPerLocalUnit(): Float {
    val matrix = skiaCanvas.localToDeviceAsMatrix33.mat
    return pixelsPerUnitOfAffine(scaleX = matrix[0], skewX = matrix[1], skewY = matrix[3], scaleY = matrix[4])
}
