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

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint

internal actual fun drawTrianglesThroughFastPath(
    canvas: Canvas,
    paint: Paint,
    positions: FloatArray,
    colors: IntArray,
    indices: ShortArray,
    vertexCount: Int,
    indexCount: Int,
    texCoords: FloatArray?,
) = false
