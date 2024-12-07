package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.implementation.manager.ViewportManagerImpl
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

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
    viewportEdgeBuffer: ScenePixel,
): Boolean = left <= viewportCenter.x + scaledHalfViewportSize.width + viewportEdgeBuffer &&
        top <= viewportCenter.y + scaledHalfViewportSize.height + viewportEdgeBuffer &&
        right >= viewportCenter.x - scaledHalfViewportSize.width - viewportEdgeBuffer &&
        bottom >= viewportCenter.y - scaledHalfViewportSize.height - viewportEdgeBuffer