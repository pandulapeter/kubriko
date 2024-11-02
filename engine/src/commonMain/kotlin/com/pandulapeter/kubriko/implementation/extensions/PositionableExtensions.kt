package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.traits.Positionable
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize

private const val VIEWPORT_EDGE_BUFFER = 50

internal fun Positionable.isVisible(
    scaledHalfViewportSize: SceneSize,
    viewportCenter: SceneOffset,
    viewportScaleFactor: Float,
): Boolean = (com.pandulapeter.kubriko.implementation.extensions.VIEWPORT_EDGE_BUFFER / viewportScaleFactor).scenePixel.let { viewportEdgeBuffer ->
    boundingBox.width.raw * viewportScaleFactor >= 1f && boundingBox.height.raw * viewportScaleFactor >= 1f &&
            left <= viewportCenter.x + scaledHalfViewportSize.width + viewportEdgeBuffer &&
            top <= viewportCenter.y + scaledHalfViewportSize.height + viewportEdgeBuffer &&
            right >= viewportCenter.x - scaledHalfViewportSize.width - viewportEdgeBuffer &&
            bottom >= viewportCenter.y - scaledHalfViewportSize.height - viewportEdgeBuffer
}

fun Positionable.angleTowards(other: Positionable): AngleRadians = (position + pivotOffset).angleTowards(other.position + other.pivotOffset)

fun Positionable.occupiesPosition(
    sceneOffset: SceneOffset,
): Boolean = sceneOffset.x.raw in left..right && sceneOffset.y.raw in top..bottom

internal fun Positionable.isAroundPosition(
    position: SceneOffset,
    range: Float,
): Boolean = (this.position - position).raw.getDistance() < range

val Positionable.left: ScenePixel get() = scale.horizontal.let { position.x + pivotOffset.x * it - boundingBox.width * it }

val Positionable.top: ScenePixel get() = scale.vertical.let { position.y + pivotOffset.y * it - boundingBox.height * it }

val Positionable.right: ScenePixel get() = scale.horizontal.let { position.x - pivotOffset.x * it + boundingBox.width * it }

val Positionable.bottom: ScenePixel get() = scale.vertical.let { position.y - pivotOffset.y * it + boundingBox.height * it }