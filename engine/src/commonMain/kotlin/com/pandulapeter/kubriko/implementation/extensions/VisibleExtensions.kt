package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

fun Visible.angleTowards(other: Visible): AngleRadians = (body.position + body.pivot).angleTowards(other.body.position + other.body.pivot)

fun Visible.transformForViewport(drawTransform: DrawTransform) {
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
    val correctionSize = bottomRight - topLeft + body.axisAlignedBoundingBox.size.let { SceneOffset(it.width, it.height) }
    if (body.axisAlignedBoundingBox.max.x < topLeft.x) {
        offset += SceneOffset(correctionSize.x, 0f.scenePixel)
    }
    if (body.axisAlignedBoundingBox.min.x > bottomRight.x) {
        offset -= SceneOffset(correctionSize.x, 0f.scenePixel)
    }
    if (body.axisAlignedBoundingBox.max.y < topLeft.y) {
        offset += SceneOffset(0f.scenePixel, correctionSize.y)
    }
    if (body.axisAlignedBoundingBox.min.y > bottomRight.y) {
        offset -= SceneOffset(0f.scenePixel, correctionSize.y)
    }
    return offset
}