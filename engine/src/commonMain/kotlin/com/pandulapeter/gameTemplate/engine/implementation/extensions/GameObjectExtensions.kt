package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible


inline fun <reified T : Trait<T>> GameObject<*>.getTrait() = traits.firstOrNull { it is T } as? T

inline fun <reified T : Trait<T>> GameObject<*>.hasTrait() = traits.any { it is T }

private const val VIEWPORT_EDGE_BUFFER = 50

// Note: Rotation is not taken into consideration
internal fun Visible.isVisible(
    scaledHalfViewportSize: Size,
    viewportOffset: Offset,
    viewportScaleFactor: Float,
) = (VIEWPORT_EDGE_BUFFER / viewportScaleFactor).let { viewportEdgeBuffer ->
    bounds.width * viewportScaleFactor >= 1f && bounds.height * viewportScaleFactor >= 1f &&
            left <= viewportOffset.x + scaledHalfViewportSize.width + viewportEdgeBuffer &&
            top <= viewportOffset.y + scaledHalfViewportSize.height + viewportEdgeBuffer &&
            right >= viewportOffset.x - scaledHalfViewportSize.width - viewportEdgeBuffer &&
            bottom >= viewportOffset.y - scaledHalfViewportSize.height - viewportEdgeBuffer
}

fun Visible.angleTowards(other: Visible) = (position + pivot).angleTowards(other.position + other.pivot)

fun Visible.occupiesPosition(
    worldCoordinates: Offset,
) = worldCoordinates.x in left..right && worldCoordinates.y in top..bottom

internal fun Visible.isAroundPosition(
    position: Offset,
    range: Float,
) = (this.position - position).getDistance() < range

val Visible.left get() = scale.width.let { position.x + pivot.x * it - bounds.width * it }

val Visible.top get() = scale.height.let { position.y + pivot.y * it - bounds.height * it }

val Visible.right get() = scale.width.let { position.x - pivot.x * it + bounds.width * it }

val Visible.bottom get() = scale.height.let { position.y - pivot.y * it + bounds.height * it }

internal fun Visible.transform(drawTransform: DrawTransform) {
    drawTransform.translate(
        left = position.x - pivot.x,
        top = position.y - pivot.y,
    )
    if (rotationDegrees != 0f) {
        drawTransform.rotate(
            degrees = rotationDegrees,
            pivot = pivot,
        )
    }
    if (scale.width != 1f || scale.height != 1f) {
        drawTransform.scale(
            scaleX = scale.width,
            scaleY = scale.height,
            pivot = pivot,
        )
    }
}