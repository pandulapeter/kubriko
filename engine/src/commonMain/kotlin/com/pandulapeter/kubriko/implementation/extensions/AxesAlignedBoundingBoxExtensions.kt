package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal fun AxisAlignedBoundingBox.isWithinViewportBounds(
    scaledHalfViewportSize: SceneSize,
    viewportCenter: SceneOffset,
): Boolean = left <= viewportCenter.x + scaledHalfViewportSize.width &&
        top <= viewportCenter.y + scaledHalfViewportSize.height &&
        right >= viewportCenter.x - scaledHalfViewportSize.width &&
        bottom >= viewportCenter.y - scaledHalfViewportSize.height