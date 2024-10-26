package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

private const val VIEWPORT_EDGE_BUFFER = 50

// Note: Rotation is not taken into consideration
internal fun Visible.isVisible(
    scaledHalfViewportSize: Size,
    viewportOffset: Offset,
    viewportScaleFactor: Float,
) = (VIEWPORT_EDGE_BUFFER / viewportScaleFactor).let { viewportEdgeBuffer ->
    size.width * viewportScaleFactor >= 1f && size.height * viewportScaleFactor >= 1f &&
            left <= viewportOffset.x + scaledHalfViewportSize.width + viewportEdgeBuffer &&
            top <= viewportOffset.y + scaledHalfViewportSize.height + viewportEdgeBuffer &&
            right >= viewportOffset.x - scaledHalfViewportSize.width - viewportEdgeBuffer &&
            bottom >= viewportOffset.y - scaledHalfViewportSize.height - viewportEdgeBuffer
}

// Note: Rotation is not taken into consideration
internal fun Visible.occupiesPosition(
    worldCoordinates: Offset,
) = worldCoordinates.x in left..right && worldCoordinates.y in top..bottom

// Note: Rotation is not taken into consideration
internal fun Visible.isAroundPosition(
    position: Offset,
    range: Float,
) = (this.position - position).getDistance() < range

private val Visible.left get() = scaleFactor.let { position.x + pivot.x * it - size.width * it }

private val Visible.top get() = scaleFactor.let { position.y + pivot.y * it - size.height * it }

private val Visible.right get() = scaleFactor.let { position.x - pivot.x * it + size.width * it }

private val Visible.bottom get() = scaleFactor.let { position.y - pivot.y * it + size.height * it }

private val Visible.scaleFactor get() = (if (this is Scalable) scaleFactor else 1f)

internal fun Visible.transform(drawTransform: DrawTransform) {
    drawTransform.translate(
        left = position.x - pivot.x,
        top = position.y - pivot.y,
    )
    if (this is Rotatable) {
        drawTransform.rotate(
            degrees = rotationDegrees,
            pivot = pivot,
        )
    }
    if (this is Scalable) {
        drawTransform.scale(
            scaleX = scaleFactor,
            scaleY = scaleFactor,
            pivot = pivot,
        )
    }
}