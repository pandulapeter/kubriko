package com.pandulapeter.kubriko.engine.implementation.extensions

import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.engine.types.SceneOffset

internal fun DrawTransform.transformViewport(
    viewportCenter: SceneOffset,
    shiftedViewportOffset: SceneOffset,
    viewportScaleFactor: Float,
) {
    translate(
        left = shiftedViewportOffset.x.raw,
        top = shiftedViewportOffset.y.raw,
    )
    scale(
        scaleX = viewportScaleFactor,
        scaleY = viewportScaleFactor,
        pivot = viewportCenter.raw,
    )
}