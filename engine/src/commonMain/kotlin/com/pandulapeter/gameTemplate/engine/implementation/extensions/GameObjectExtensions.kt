package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

private const val VIEWPORT_SIZE_BUFFER = 100

internal fun Visible.isVisible(
    scaledViewportSize: Size,
    viewportOffset: Offset,
    viewportScaleFactor: Float,
) = if (this is Scalable) (size.width * scaleFactor).let { scaledWidth ->
    (size.height * scaleFactor).let { scaledHeight ->
        scaledWidth * viewportScaleFactor >= 1f && scaledHeight * viewportScaleFactor >= 1f &&
                position.x - pivot.x + scaledWidth >= viewportOffset.x - scaledViewportSize.width / 2f - VIEWPORT_SIZE_BUFFER &&
                position.x - pivot.x - scaledWidth <= viewportOffset.x + scaledViewportSize.width / 2f + VIEWPORT_SIZE_BUFFER &&
                position.y - pivot.y + scaledHeight >= viewportOffset.y - scaledViewportSize.height / 2f - VIEWPORT_SIZE_BUFFER &&
                position.y - pivot.y - scaledHeight <= viewportOffset.y + scaledViewportSize.height / 2f + VIEWPORT_SIZE_BUFFER
    }
} else size.width * viewportScaleFactor >= 1f && size.height * viewportScaleFactor >= 1f &&
        position.x - pivot.x + size.width >= viewportOffset.x - scaledViewportSize.width / 2f - VIEWPORT_SIZE_BUFFER &&
        position.x - pivot.x - size.width <= viewportOffset.x + scaledViewportSize.width / 2f + VIEWPORT_SIZE_BUFFER &&
        position.y - pivot.y + size.height >= viewportOffset.y - scaledViewportSize.height / 2f - VIEWPORT_SIZE_BUFFER &&
        position.y - pivot.y - size.height <= viewportOffset.y + scaledViewportSize.height / 2f + VIEWPORT_SIZE_BUFFER


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