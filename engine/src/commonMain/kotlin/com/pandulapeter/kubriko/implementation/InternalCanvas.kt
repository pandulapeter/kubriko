package com.pandulapeter.kubriko.implementation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.transform
import com.pandulapeter.kubriko.implementation.extensions.transformViewport
import com.pandulapeter.kubriko.types.SceneOffset

@Composable
internal fun InternalCanvas(
    modifier: Modifier,
    content: Map<Int?, Pair<Modifier?, List<Visible>>>,
    viewportCenter: SceneOffset,
    viewportScaleFactor: Float,
    overlayActors: List<Overlay>,
    getGameTime: () -> Long,
) = Box(
    modifier = modifier then (content[null]?.first ?: Modifier),
) {
    content.keys.sortedByDescending { it }.forEach { canvasIndex ->
        Canvas(
            modifier = if (canvasIndex == null) Modifier.fillMaxSize() else (content[canvasIndex]?.first ?: Modifier),
            onDraw = {
                getGameTime() // This line invalidates the Canvas (causing a refresh) on every frame
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportCenter = viewportCenter,
                            shiftedViewportOffset = (size / 2f) - viewportCenter,
                            viewportScaleFactor = viewportScaleFactor,
                        )
                    },
                    drawBlock = {
                        content[canvasIndex]?.second
                            .orEmpty()
                            .sortedByDescending { it.drawingOrder }
                            .forEach { visible ->
                                withTransform(
                                    transformBlock = { visible.transform(this) },
                                    drawBlock = { visible.draw(this) }
                                )
                            }
                        overlayActors
                            .filter { it.canvasIndex == canvasIndex }
                            .sortedByDescending { it.overlayDrawingOrder }
                            .forEach { it.drawToViewport(this) }
                    }
                )
            }
        )
    }
}