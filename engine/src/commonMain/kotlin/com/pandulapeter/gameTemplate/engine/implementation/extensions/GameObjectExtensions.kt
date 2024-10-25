package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

internal fun Visible.isVisible(
    viewportSize: Size,
    viewportOffset: Offset,
    viewportScaleFactor: Float,
) = (size.width * ((this as? Scalable)?.scaleFactor ?: 1f)).let { scaledWidth ->
    (size.height * ((this as? Scalable)?.scaleFactor ?: 1f)).let { scaledHeight ->
        scaledWidth * viewportScaleFactor >= 1f && scaledHeight * viewportScaleFactor >= 1f &&
                position.x - pivot.x + scaledWidth >= viewportOffset.x - viewportSize.width / 2f &&
                position.x - pivot.x - scaledWidth <= viewportOffset.x + viewportSize.width - viewportSize.width / 2f &&
                position.y - pivot.y + scaledHeight >= viewportOffset.y - viewportSize.height / 2f &&
                position.y - pivot.y - scaledHeight <= viewportOffset.y + viewportSize.height - viewportSize.height / 2f
    }
}


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