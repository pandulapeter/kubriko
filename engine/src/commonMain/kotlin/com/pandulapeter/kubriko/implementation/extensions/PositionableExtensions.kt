package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.types.SceneOffset

fun Positionable.checkAxisAlignedBoundingBoxOverlap(
    sceneOffset: SceneOffset,
): Boolean =
    sceneOffset.x.raw in body.axisAlignedBoundingBox.left..body.axisAlignedBoundingBox.right && sceneOffset.y.raw in body.axisAlignedBoundingBox.top..body.axisAlignedBoundingBox.bottom

fun Positionable.checkAxisAlignedBoundingBoxOverlap(other: Positionable): Boolean {
    val overlapTopLeft = SceneOffset(
        x = maxOf(body.axisAlignedBoundingBox.left, other.body.axisAlignedBoundingBox.left),
        y = maxOf(body.axisAlignedBoundingBox.top, other.body.axisAlignedBoundingBox.top)
    )
    val overlapBottomRight = SceneOffset(
        x = minOf(body.axisAlignedBoundingBox.right, other.body.axisAlignedBoundingBox.right),
        y = minOf(body.axisAlignedBoundingBox.bottom, other.body.axisAlignedBoundingBox.bottom)
    )
    return !(overlapTopLeft.x >= overlapBottomRight.x || overlapTopLeft.y >= overlapBottomRight.y)
}