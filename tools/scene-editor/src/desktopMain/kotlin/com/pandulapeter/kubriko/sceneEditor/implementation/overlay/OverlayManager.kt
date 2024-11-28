package com.pandulapeter.kubriko.sceneEditor.implementation.overlay

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.transformForViewport
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.implementation.EditorController
import com.pandulapeter.kubriko.types.SceneOffset

internal class OverlayManager(editorController: EditorController) : Manager(), Overlay, Unique {

    private val gameActorManager by lazy { editorController.kubriko.require<ActorManager>() }
    private val gameViewportManager by lazy { editorController.kubriko.require<ViewportManager>() }
    private val colorBack = Color.Black.copy(alpha = 0.75f)
    private val colorFront = Color.White
    override val overlayDrawingOrder = Float.MIN_VALUE

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.require<ActorManager>().add(this)
    }

    override fun DrawScope.drawToViewport() {
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
                            width = 5f / scaleFactor,
                            join = StrokeJoin.Round,
                        )
                        val strokeFront = Stroke(
                            width = 2f / scaleFactor,
                            join = StrokeJoin.Round,
                        )
                        gameActorManager.visibleActorsWithinViewport.value.forEach { visible ->
                            withTransform(
                                transformBlock = { visible.transformForViewport(this) },
                                drawBlock = {
                                    with(visible.body) {
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
}