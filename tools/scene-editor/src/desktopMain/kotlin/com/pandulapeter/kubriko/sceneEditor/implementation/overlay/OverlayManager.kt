package com.pandulapeter.kubriko.sceneEditor.implementation.overlay

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.transformForViewport
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.implementation.EditorController
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.max
import kotlin.math.min

internal class OverlayManager(
    private val editorController: EditorController,
) : Manager(), Dynamic, Overlay, Unique {
    private val gameViewportManager by lazy { editorController.kubriko.require<ViewportManager>() }
    private val colorBack = Color.Black.copy(alpha = 0.25f)
    private val colorFront = Color.White.copy(alpha = 0.75f)
    override val overlayDrawingOrder = Float.MIN_VALUE
    private var alpha = 0f

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.require<ActorManager>().add(this)
    }

    override fun update(deltaTimeInMillis: Float) {
        if (editorController.selectedUpdatableActor.value.first == null) {
            if (alpha > 0) {
                alpha = max(0f, alpha - deltaTimeInMillis * HIGHLIGHT_BACKGROUND_FADE_SPEED)
            }
        } else {
            if (alpha < HIGHLIGHT_BACKGROUND_ALPHA) {
                alpha = min(HIGHLIGHT_BACKGROUND_ALPHA, alpha + deltaTimeInMillis * HIGHLIGHT_BACKGROUND_FADE_SPEED)
            }
        }
    }

    override fun DrawScope.drawToViewport() {
        drawRect(
            color = Color.Black.copy(alpha),
            size = size,
        )
        editorController.selectedUpdatableActor.value.first?.let { positionable ->
            gameViewportManager.cameraPosition.value.let { viewportCenter ->
                gameViewportManager.scaleFactor.value.let { scaleFactor ->
                    withTransform(
                        transformBlock = {
                            transformViewport(
                                viewportCenter = viewportCenter,
                                shiftedViewportOffset = (gameViewportManager.size.value / 2f) - viewportCenter,
                                viewportScaleFactor = scaleFactor,
                            )
                        },
                        drawBlock = {
                            val strokeBack = Stroke(
                                width = 8f / scaleFactor,
                                join = StrokeJoin.Round,
                            )
                            val strokeFront = Stroke(
                                width = 4f / scaleFactor,
                                join = StrokeJoin.Round,
                            )
                            // TODO: Fade on hover
//                        gameActorManager.visibleActorsWithinViewport.value.forEach { visible ->
//                            withTransform(
//                                transformBlock = { visible.transformForViewport(this) },
//                                drawBlock = {
//                                    with(visible.body) {
//                                        drawDebugBounds(colorBack, strokeBack)
//                                        drawDebugBounds(colorFront, strokeFront)
//                                    }
//                                },
//                            )
//                        }
                            if (positionable is Visible) { // TODO: Handle else branch
                                withTransform(
                                    transformBlock = { positionable.transformForViewport(this) },
                                    drawBlock = {
                                        with(positionable) {
                                            draw()
                                        }
                                        with(positionable.body) {
                                            drawDebugBounds(colorBack, strokeBack)
                                            drawDebugBounds(colorFront, strokeFront)
                                        }
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun DrawTransform.transformViewport(
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

    companion object {
        private const val HIGHLIGHT_BACKGROUND_ALPHA = 0.5f
        private const val HIGHLIGHT_BACKGROUND_FADE_SPEED = 0.002f
    }
}