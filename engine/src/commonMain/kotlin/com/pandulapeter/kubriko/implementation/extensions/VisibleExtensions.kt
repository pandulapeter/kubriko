package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

fun Visible.angleTowards(other: Visible): AngleRadians = (body.position + body.pivot).angleTowards(other.body.position + other.body.pivot)

internal fun Visible.transform(drawTransform: DrawTransform) {
    drawTransform.translate(
        left = (body.position.x - body.pivot.x).raw,
        top = (body.position.y - body.pivot.y).raw,
    )
    if (body.rotation != AngleRadians.Zero) {
        drawTransform.rotate(
            degrees = body.rotation.deg,
            pivot = body.pivot.raw,
        )
    }
    if (body.scale != Scale.Unit) {
        drawTransform.scale(
            scaleX = body.scale.horizontal,
            scaleY = body.scale.vertical,
            pivot = body.pivot.raw,
        )
    }
}

fun Visible.wrapWithin(topLeft: SceneOffset, bottomRight: SceneOffset): SceneOffset {
    var offset = body.position
    if (body.axisAlignedBoundingBox.min.x < topLeft.x) {
        offset = SceneOffset(bottomRight.x + body.axisAlignedBoundingBox.size.width, offset.y)
    }
    if (body.axisAlignedBoundingBox.max.x > bottomRight.x) {
        offset = SceneOffset(topLeft.x - body.axisAlignedBoundingBox.size.width, offset.y)
    }
    if (body.axisAlignedBoundingBox.min.y < topLeft.y) {
        offset = SceneOffset(offset.x, bottomRight.y + body.axisAlignedBoundingBox.size.height)
    }
    if (body.axisAlignedBoundingBox.max.y > bottomRight.y) {
        offset = SceneOffset(offset.x, topLeft.y - body.axisAlignedBoundingBox.size.height)
    }
    return offset
}