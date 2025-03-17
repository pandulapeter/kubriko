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

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.manager.ViewportManagerImpl
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit

fun AxisAlignedBoundingBox.isWithinViewportBounds(
    viewportManager: ViewportManager,
): Boolean = isWithinViewportBounds(
    scaledHalfViewportSize = SceneSize(viewportManager.size.value / (viewportManager.scaleFactor.value * 2)),
    viewportCenter = viewportManager.cameraPosition.value,
    viewportEdgeBuffer = (viewportManager as ViewportManagerImpl).viewportEdgeBuffer,
)

internal fun AxisAlignedBoundingBox.isWithinViewportBounds(
    scaledHalfViewportSize: SceneSize,
    viewportCenter: SceneOffset,
    viewportEdgeBuffer: SceneUnit,
): Boolean = left <= viewportCenter.x + scaledHalfViewportSize.width + viewportEdgeBuffer &&
        top <= viewportCenter.y + scaledHalfViewportSize.height + viewportEdgeBuffer &&
        right >= viewportCenter.x - scaledHalfViewportSize.width - viewportEdgeBuffer &&
        bottom >= viewportCenter.y - scaledHalfViewportSize.height - viewportEdgeBuffer

fun AxisAlignedBoundingBox.isOverlapping(other: AxisAlignedBoundingBox): Boolean {
    val overlapTopLeft = SceneOffset(
        x = maxOf(left, other.left),
        y = maxOf(top, other.top)
    )
    val overlapBottomRight = SceneOffset(
        x = minOf(right, other.right),
        y = minOf(bottom, other.bottom)
    )
    return !(overlapTopLeft.x >= overlapBottomRight.x || overlapTopLeft.y >= overlapBottomRight.y)
}