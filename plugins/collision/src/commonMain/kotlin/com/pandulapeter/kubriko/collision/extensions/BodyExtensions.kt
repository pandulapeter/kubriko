package com.pandulapeter.kubriko.collision.extensions

import com.pandulapeter.kubriko.actor.body.Body
import com.pandulapeter.kubriko.extensions.isInside

fun Body.isOverlapping(
    other: Body
) = if (axisAlignedBoundingBox.isInside(other.axisAlignedBoundingBox)) {
    // TODO: Perform detailed check using the collision masks
    true
} else {
    false
}