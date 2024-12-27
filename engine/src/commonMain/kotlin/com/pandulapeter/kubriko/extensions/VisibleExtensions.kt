package com.pandulapeter.kubriko.extensions

import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale

fun Visible.transformForViewport(drawTransform: DrawTransform) {
    drawTransform.translate(
        left = (body.position.x - body.pivot.x).raw,
        top = (body.position.y - body.pivot.y).raw,
    )
    if (body.rotation != AngleRadians.Zero) {
        drawTransform.rotate(
            degrees = body.rotation.deg.normalized,
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