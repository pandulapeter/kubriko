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

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.manager.ViewportManagerImpl
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Checks if this bounding box is currently visible within the viewport.
 *
 * @param viewportManager The [ViewportManager] used for the visibility check.
 */
fun AxisAlignedBoundingBox.isWithinViewportBounds(
    viewportManager: ViewportManager,
): Boolean = (viewportManager as ViewportManagerImpl).let { viewportManagerImpl ->
    isWithinViewportBounds(
        scaledHalfViewportSize = SceneSize(viewportManagerImpl.size.value / (viewportManagerImpl.currentScaleFactor() * 2)),
        viewportCenter = viewportManagerImpl.cameraPosition.value,
        viewportEdgeBuffer = viewportManagerImpl.viewportEdgeBuffer,
    )
}

/**
 * Raw-float math, so the per-frame checks games run through the public overload don't box SceneUnits through the
 * operator chain.
 */
internal fun AxisAlignedBoundingBox.isWithinViewportBounds(
    scaledHalfViewportSize: SceneSize,
    viewportCenter: SceneOffset,
    viewportEdgeBuffer: SceneUnit,
): Boolean {
    val horizontalReach = scaledHalfViewportSize.width.raw + viewportEdgeBuffer.raw
    val verticalReach = scaledHalfViewportSize.height.raw + viewportEdgeBuffer.raw
    return minXRaw <= viewportCenter.x.raw + horizontalReach &&
            minYRaw <= viewportCenter.y.raw + verticalReach &&
            maxXRaw >= viewportCenter.x.raw - horizontalReach &&
            maxYRaw >= viewportCenter.y.raw - verticalReach
}

/**
 * Checks if this bounding box overlaps with [other]. Boxes that merely touch at an edge do not count as overlapping.
 *
 * It compares raw floats and never boxes, since it is the broad-phase test every collision and raycast query funnels
 * through.
 */
fun AxisAlignedBoundingBox.isOverlapping(other: AxisAlignedBoundingBox): Boolean =
    minXRaw < other.maxXRaw && other.minXRaw < maxXRaw && minYRaw < other.maxYRaw && other.minYRaw < maxYRaw