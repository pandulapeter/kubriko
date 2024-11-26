package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

internal fun Positionable.isWithinViewportBounds(
    scaledHalfViewportSize: SceneSize,
    viewportCenter: SceneOffset,
    viewportEdgeBuffer: ScenePixel,
): Boolean = body.axisAlignedBoundingBox.left <= viewportCenter.x + scaledHalfViewportSize.width + viewportEdgeBuffer &&
        body.axisAlignedBoundingBox.top <= viewportCenter.y + scaledHalfViewportSize.height + viewportEdgeBuffer &&
        body.axisAlignedBoundingBox.right >= viewportCenter.x - scaledHalfViewportSize.width - viewportEdgeBuffer &&
        body.axisAlignedBoundingBox.bottom >= viewportCenter.y - scaledHalfViewportSize.height - viewportEdgeBuffer

fun Positionable.angleTowards(other: Positionable): AngleRadians = (body.position).angleTowards(other.body.position)

fun Positionable.occupiesPosition(
    sceneOffset: SceneOffset,
): Boolean =
    sceneOffset.x.raw in body.axisAlignedBoundingBox.left..body.axisAlignedBoundingBox.right && sceneOffset.y.raw in body.axisAlignedBoundingBox.top..body.axisAlignedBoundingBox.bottom

fun Positionable.isAroundPosition(
    position: SceneOffset,
    range: ScenePixel,
): Boolean = (this.body.position - position).raw.getDistance().scenePixel < range

// TODO: AABB - might be merged with the implementation from the physics module
// TODO: Fine-tune with better collision masks
fun Positionable.isOverlapping(other: Positionable): Boolean {
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