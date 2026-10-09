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

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.manager.MetadataManager
import kotlin.math.exp

/**
 * Returns true only if the cursor was actually moved, meaning that a synthetic move event will follow.
 * Callers rely on this contract to know whether the next move event should be filtered out.
 */
internal expect fun setPointerPosition(
    platform: MetadataManager.Platform,
    offset: Offset,
    densityMultiplier: Float,
): Boolean

internal expect fun Modifier.gestureDetector(
    onDragDetected: (Offset) -> Unit,
    onZoomDetected: (Offset, Float) -> Unit,
): Modifier

internal expect val isMultiTouchEnabled: Boolean

/**
 * Turns a scroll delta into a zoom factor that is always positive and finite, and symmetric: scrolling by `d` and
 * then by `-d` returns to the starting scale. The exponent is clamped because a Float `exp` underflows to 0 and
 * overflows to Infinity well within the deltas a coalesced fast spin can report.
 */
internal fun scrollZoomFactor(scrollDelta: Float, sensitivity: Float): Float {
    if (!scrollDelta.isFinite()) return 1f
    return exp((-scrollDelta * sensitivity).coerceIn(-MAX_ZOOM_EXPONENT, MAX_ZOOM_EXPONENT))
}

private const val MAX_ZOOM_EXPONENT = 10f
