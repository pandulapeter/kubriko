package com.pandulapeter.kubriko.extensions

import androidx.compose.ui.graphics.drawscope.DrawTransform
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

internal fun DrawTransform.transformViewport(
    viewportCenter: SceneOffset,
    shiftedViewportOffset: SceneOffset,
    viewportScaleFactor: Scale,
) {
    translate(
        left = shiftedViewportOffset.x.raw,
        top = shiftedViewportOffset.y.raw,
    )
    scale(
        scaleX = viewportScaleFactor.horizontal,
        scaleY = viewportScaleFactor.vertical,
        pivot = viewportCenter.raw,
    )
}