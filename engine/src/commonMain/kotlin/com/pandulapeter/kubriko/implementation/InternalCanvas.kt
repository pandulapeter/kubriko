package com.pandulapeter.kubriko.implementation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.transform
import com.pandulapeter.kubriko.implementation.extensions.transformViewport
import com.pandulapeter.kubriko.types.SceneOffset

@Composable
internal fun InternalCanvas(
    canvasModifiers: Map<Int?, Modifier>,
    viewportCenter: SceneOffset,
    viewportScaleFactor: Float,
    visibleActorsWithinViewport: List<Visible>,
    overlayActors: List<Overlay>,
    getGameTime: () -> Long,
) = Box(
    modifier = canvasModifiers[null] ?: Modifier,
) {
    canvasModifiers.keys.sortedByDescending { it }.forEach { canvasIndex ->
        Canvas(
            modifier = (if (canvasIndex == null) Modifier else (canvasModifiers[canvasIndex] ?: Modifier)).fillMaxSize(),
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
                        visibleActorsWithinViewport
                            .filter { it.canvasIndex == canvasIndex }
                            .sortedByDescending { it.drawingOrder }
                            .forEach { visible ->
                                withTransform(
                                    transformBlock = { visible.transform(this) },
                                    drawBlock = {
                                        with(visible) {
                                            clipRect(
                                                right = boundingBox.width.raw,
                                                bottom = boundingBox.height.raw,
                                            ) {
                                                draw()
                                            }
                                        }
                                    }
                                )
                            }
                    }
                )
                overlayActors
                    .filter { it.canvasIndex == canvasIndex }
                    .sortedByDescending { it.overlayDrawingOrder }
                    .forEach { with(it) { drawToViewport() } }
            }
        )
    }
}