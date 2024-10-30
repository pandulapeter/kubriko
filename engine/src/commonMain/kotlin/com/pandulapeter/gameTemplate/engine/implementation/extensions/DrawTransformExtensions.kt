package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates

internal fun DrawTransform.transformViewport(
    viewportCenter: WorldCoordinates,
    shiftedViewportOffset: WorldCoordinates,
    viewportScaleFactor: Float,
) {
    translate(
        left = shiftedViewportOffset.x,
        top = shiftedViewportOffset.y,
    )
    scale(
        scaleX = viewportScaleFactor,
        scaleY = viewportScaleFactor,
        pivot = Offset(viewportCenter.x, viewportCenter.y),
    )
}