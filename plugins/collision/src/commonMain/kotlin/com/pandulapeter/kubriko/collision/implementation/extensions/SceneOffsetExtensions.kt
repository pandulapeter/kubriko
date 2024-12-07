package com.pandulapeter.kubriko.collision.implementation.extensions

import com.pandulapeter.kubriko.actor.body.Body
import com.pandulapeter.kubriko.implementation.extensions.isWithin
import com.pandulapeter.kubriko.types.SceneOffset

fun SceneOffset.isWithin(
    other: Body
) = if (isWithin(other.axisAlignedBoundingBox)) {
    // TODO: Perform detailed check using the collision masks
    true
} else {
    false
}