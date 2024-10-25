package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

private const val VIEWPORT_EDGE_BUFFER = 50

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

internal fun Visible.occupiesPosition(
    worldCoordinates: Offset,
) = worldCoordinates.x > left && worldCoordinates.x < right && worldCoordinates.y > top && worldCoordinates.y < bottom

private val Visible.left get() = position.x + pivot.x - size.width * (if (this is Scalable) scaleFactor else 1f)

private val Visible.top get() = position.y + pivot.y - size.height * (if (this is Scalable) scaleFactor else 1f)

private val Visible.right get() = position.x - pivot.x + size.width * (if (this is Scalable) scaleFactor else 1f)

private val Visible.bottom get() = position.y - pivot.y + size.height * (if (this is Scalable) scaleFactor else 1f)

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