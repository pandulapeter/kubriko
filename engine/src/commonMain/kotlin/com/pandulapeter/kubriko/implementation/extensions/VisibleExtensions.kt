package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

internal fun Visible.transform(drawTransform: DrawTransform) {
    drawTransform.translate(
        left = (position.x - pivotOffset.x).raw,
        top = (position.y - pivotOffset.y).raw,
    )
    if (rotation != com.pandulapeter.kubriko.types.AngleRadians.Zero) {
        drawTransform.rotate(
            degrees = rotation.deg,
            pivot = pivotOffset.raw,
        )
    }
    if (scale != Scale.Unit) {
        drawTransform.scale(
            scaleX = scale.horizontal,
            scaleY = scale.vertical,
            pivot = pivotOffset.raw,
        )
    }
}

fun Visible.wrapWithin(topLeft: SceneOffset, bottomRight: SceneOffset): SceneOffset {
    var offset = position
    if (offset.x + boundingBox.width < topLeft.x) {
        offset = SceneOffset(bottomRight.x + boundingBox.width, offset.y)
    }
    if (offset.x - boundingBox.width > bottomRight.x) {
        offset = SceneOffset(topLeft.x - boundingBox.width, offset.y)
    }
    if (offset.y + boundingBox.height < topLeft.y) {
        offset = SceneOffset(offset.x, bottomRight.y + boundingBox.height)
    }
    if (offset.y - boundingBox.height > bottomRight.y) {
        offset = SceneOffset(offset.x, topLeft.y - boundingBox.height)
    }
    return offset
}