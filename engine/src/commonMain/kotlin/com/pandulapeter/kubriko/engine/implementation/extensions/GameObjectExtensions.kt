package com.pandulapeter.kubriko.engine.implementation.extensions

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.engine.traits.Positionable
import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.types.Scale
import com.pandulapeter.kubriko.engine.types.WorldCoordinates

private const val VIEWPORT_EDGE_BUFFER = 50

// Note: Rotation is not taken into consideration
internal fun Visible.isVisible(
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

fun Visible.angleTowards(other: Visible) = (position + pivotOffset).angleTowards(other.position + other.pivotOffset)

fun Positionable.occupiesPosition(
    worldCoordinates: WorldCoordinates,
) = worldCoordinates.x in left..right && worldCoordinates.y in top..bottom

internal fun Visible.isAroundPosition(
    position: WorldCoordinates,
    range: Float,
) = (this.position - position).rawOffset.getDistance() < range

val Positionable.left get() = scale.horizontal.let { position.x + pivotOffset.x * it - boundingBox.width * it }

val Positionable.top get() = scale.vertical.let { position.y + pivotOffset.y * it - boundingBox.height * it }

val Positionable.right get() = scale.horizontal.let { position.x - pivotOffset.x * it + boundingBox.width * it }

val Positionable.bottom get() = scale.vertical.let { position.y - pivotOffset.y * it + boundingBox.height * it }

internal fun Visible.transform(drawTransform: DrawTransform) {
    drawTransform.translate(
        left = position.x - pivotOffset.x,
        top = position.y - pivotOffset.y,
    )
    if (rotation.normalized != 0f) {
        drawTransform.rotate(
            degrees = rotation.normalized,
            pivot = pivotOffset.rawOffset,
        )
    }
    if (scale != Scale.Unit) {
        drawTransform.scale(
            scaleX = scale.horizontal,
            scaleY = scale.vertical,
            pivot = pivotOffset.rawOffset,
        )
    }
}