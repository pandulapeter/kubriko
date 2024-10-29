package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.types.MapCoordinates
import com.pandulapeter.gameTemplate.engine.types.Scale


inline fun <reified T : Trait<T>> GameObject<*>.getTrait() = allTraits[T::class] as? T

inline fun <reified T : Trait<T>> GameObject<*>.hasTrait() = allTraits[T::class] != null

inline fun <reified T : Trait<T>> GameObject<*>.trait() = allTraits[T::class] as? T ?: throw IllegalStateException("Unregistered trait ${T::class.simpleName}")

private const val VIEWPORT_EDGE_BUFFER = 50

// Note: Rotation is not taken into consideration
internal fun Visible.isVisible(
    scaledHalfViewportSize: Size,
    viewportCenter: MapCoordinates,
    viewportScaleFactor: Float,
) = (VIEWPORT_EDGE_BUFFER / viewportScaleFactor).let { viewportEdgeBuffer ->
    boundingBox.width * viewportScaleFactor >= 1f && boundingBox.height * viewportScaleFactor >= 1f &&
            left <= viewportCenter.x + scaledHalfViewportSize.width + viewportEdgeBuffer &&
            top <= viewportCenter.y + scaledHalfViewportSize.height + viewportEdgeBuffer &&
            right >= viewportCenter.x - scaledHalfViewportSize.width - viewportEdgeBuffer &&
            bottom >= viewportCenter.y - scaledHalfViewportSize.height - viewportEdgeBuffer
}

fun Visible.angleTowards(other: Visible) = (position + pivotOffset).angleTowards(other.position + other.pivotOffset)

fun Visible.occupiesPosition(
    worldCoordinates: MapCoordinates,
) = worldCoordinates.x in left..right && worldCoordinates.y in top..bottom

internal fun Visible.isAroundPosition(
    position: MapCoordinates,
    range: Float,
) = (this.position - position).rawOffset.getDistance() < range

val Visible.left get() = scale.horizontal.let { position.x + pivotOffset.x * it - boundingBox.width * it }

val Visible.top get() = scale.vertical.let { position.y + pivotOffset.y * it - boundingBox.height * it }

val Visible.right get() = scale.horizontal.let { position.x - pivotOffset.x * it + boundingBox.width * it }

val Visible.bottom get() = scale.vertical.let { position.y - pivotOffset.y * it + boundingBox.height * it }

internal fun Visible.transform(drawTransform: DrawTransform) {
    drawTransform.translate(
        left = position.x - pivotOffset.x,
        top = position.y - pivotOffset.y,
    )
    if (rotationDegrees.normalized != 0f) {
        drawTransform.rotate(
            degrees = rotationDegrees.normalized,
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