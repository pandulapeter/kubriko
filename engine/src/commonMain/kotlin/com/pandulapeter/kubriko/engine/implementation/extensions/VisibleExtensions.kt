package com.pandulapeter.kubriko.engine.implementation.extensions

import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.types.AngleRadians
import com.pandulapeter.kubriko.engine.types.Scale

internal fun Visible.transform(drawTransform: DrawTransform) {
    drawTransform.translate(
        left = (position.x - pivotOffset.x).raw,
        top = (position.y - pivotOffset.y).raw,
    )
    if (rotation != AngleRadians.Zero) {
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