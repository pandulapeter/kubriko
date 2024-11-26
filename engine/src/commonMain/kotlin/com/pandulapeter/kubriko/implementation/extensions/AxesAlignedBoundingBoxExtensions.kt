package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

internal fun AxisAlignedBoundingBox.isWithinViewportBounds(
    scaledHalfViewportSize: SceneSize,
    viewportCenter: SceneOffset,
    viewportEdgeBuffer: ScenePixel,
): Boolean = left <= viewportCenter.x + scaledHalfViewportSize.width + viewportEdgeBuffer &&
        top <= viewportCenter.y + scaledHalfViewportSize.height + viewportEdgeBuffer &&
        right >= viewportCenter.x - scaledHalfViewportSize.width - viewportEdgeBuffer &&
        bottom >= viewportCenter.y - scaledHalfViewportSize.height - viewportEdgeBuffer