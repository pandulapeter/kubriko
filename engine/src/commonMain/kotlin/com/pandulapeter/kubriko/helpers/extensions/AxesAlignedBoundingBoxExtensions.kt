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
): Boolean = isWithinViewportBounds(
    scaledHalfViewportSize = SceneSize(viewportManager.size.value / (viewportManager.scaleFactor.value * 2)),
    viewportCenter = viewportManager.cameraPosition.value,
    viewportEdgeBuffer = (viewportManager as ViewportManagerImpl).viewportEdgeBuffer,
)

// Raw-float math: this runs for every actor on every visibility refresh, and the SceneUnit
// operator chain would box through the generic comparisons.
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
 */
// Raw-float math: this is the broad-phase test every collision and raycast query funnels through,
// so it must not box SceneUnits through generic minOf/maxOf.
fun AxisAlignedBoundingBox.isOverlapping(other: AxisAlignedBoundingBox): Boolean =
    minXRaw < other.maxXRaw && other.minXRaw < maxXRaw && minYRaw < other.maxYRaw && other.minYRaw < maxYRaw