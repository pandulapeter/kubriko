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