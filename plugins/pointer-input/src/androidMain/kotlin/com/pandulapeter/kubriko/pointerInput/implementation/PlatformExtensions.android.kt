/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput.implementation

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.kubriko.manager.MetadataManager

internal actual fun setPointerPosition(
    platform: MetadataManager.Platform,
    offset: Offset,
    densityMultiplier: Float,
) = Unit

internal actual fun Modifier.gestureDetector(
    onDragDetected: (Offset) -> Unit,
    onZoomDetected: (Offset, Float) -> Unit,
) = pointerInput(Unit) {
    detectTransformGestures { centroid, pan, zoom, _ ->
        onDragDetected(pan)
        onZoomDetected(centroid, zoom)
    }
}

internal actual val isMultiTouchEnabled = true