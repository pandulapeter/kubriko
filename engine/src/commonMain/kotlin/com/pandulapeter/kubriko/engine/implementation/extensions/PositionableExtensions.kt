package com.pandulapeter.kubriko.engine.implementation.extensions

import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.engine.traits.Positionable
import com.pandulapeter.kubriko.engine.types.WorldCoordinates

private const val VIEWPORT_EDGE_BUFFER = 50

internal fun Positionable.isVisible(
    scaledHalfViewportSize: Size,
    viewportCenter: WorldCoordinates,
    viewportScaleFactor: Float,
) = (VIEWPORT_EDGE_BUFFER / viewportScaleFactor).let { viewportEdgeBuffer ->
    boundingBox.width * viewportScaleFactor >= 1f && boundingBox.height * viewportScaleFactor >= 1f &&
            left <= viewportCenter.x + scaledHalfViewportSize.width + viewportEdgeBuffer &&
            top <= viewportCenter.y + scaledHalfViewportSize.height + viewportEdgeBuffer &&
            right >= viewportCenter.x - scaledHalfViewportSize.width - viewportEdgeBuffer &&
            bottom >= viewportCenter.y - scaledHalfViewportSize.height - viewportEdgeBuffer
}

fun Positionable.angleTowards(other: Positionable) = (position + pivotOffset).angleTowards(other.position + other.pivotOffset)

fun Positionable.occupiesPosition(
    worldCoordinates: WorldCoordinates,
) = worldCoordinates.x in left..right && worldCoordinates.y in top..bottom

internal fun Positionable.isAroundPosition(
    position: WorldCoordinates,
    range: Float,
) = (this.position - position).rawOffset.getDistance() < range

val Positionable.left get() = scale.horizontal.let { position.x + pivotOffset.x * it - boundingBox.width * it }

val Positionable.top get() = scale.vertical.let { position.y + pivotOffset.y * it - boundingBox.height * it }

val Positionable.right get() = scale.horizontal.let { position.x - pivotOffset.x * it + boundingBox.width * it }

val Positionable.bottom get() = scale.vertical.let { position.y - pivotOffset.y * it + boundingBox.height * it }