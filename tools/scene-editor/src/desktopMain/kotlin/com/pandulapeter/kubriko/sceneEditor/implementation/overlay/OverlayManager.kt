/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.overlay

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.alphaMultiplier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.minus
import com.pandulapeter.kubriko.extensions.transformForViewport
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.implementation.EditorController
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.max
import kotlin.math.min

internal class OverlayManager(
    private val editorController: EditorController,
) : Manager(), Dynamic, Overlay, Unique {
    private val gameViewportManager by lazy { editorController.kubriko.get<ViewportManager>() }
    private val colorBack = Color.Black.copy(alpha = 0.25f)
    private val colorFront = Color.White.copy(alpha = 0.75f)
    override val overlayDrawingOrder = Float.MIN_VALUE
    private var alpha = 0f

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(this)
        combine(
            editorController.selectedUpdatableActor,
            editorController.selectedTypeId,
        ) { (selectedInstance, _), selectedTypeId ->
            selectedInstance to selectedTypeId
        }.onEach { (_, selectedTypeId) ->
            editorController.previewOverlayActor = selectedTypeId?.let {
                editorController.serializationManager.getMetadata(selectedTypeId)?.instantiate?.invoke(SceneOffset.Zero)?.restore()
            }
        }.launchIn(scope)
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        if (editorController.selectedUpdatableActor.value.first == null) {
            if (alpha > 0) {
                alpha = max(0f, alpha - deltaTimeInMilliseconds * HIGHLIGHT_BACKGROUND_FADE_SPEED)
            }
        } else {
            if (alpha < HIGHLIGHT_BACKGROUND_ALPHA) {
                alpha = min(HIGHLIGHT_BACKGROUND_ALPHA, alpha + deltaTimeInMilliseconds * HIGHLIGHT_BACKGROUND_FADE_SPEED)
            }
        }
        editorController.previewOverlayActor?.body?.position = editorController.mouseSceneOffset.value
    }

    override fun DrawScope.drawToViewport() {
        drawRect(
            color = Color.Black.copy(alpha),
            size = size,
        )
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
                        editorController.selectedUpdatableActor.value.first?.let { highlighted ->
                            val strokeBack = Stroke(
                                width = 16f / (scaleFactor.horizontal + scaleFactor.vertical),
                                join = StrokeJoin.Round,
                            )
                            val strokeFront = Stroke(
                                width = 8f / (scaleFactor.horizontal + scaleFactor.vertical),
                                join = StrokeJoin.Round,
                            )
                            if (highlighted is Visible) { // TODO: Handle else branch
                                withTransform(
                                    transformBlock = { highlighted.transformForViewport(this) },
                                    drawBlock = {
                                        with(highlighted) {
                                            clipRect(
                                                right = body.size.width.raw,
                                                bottom = body.size.height.raw
                                            ) {
                                                draw()
                                            }
                                        }
                                        with(highlighted.body) {
                                            drawDebugBounds(colorBack, strokeBack)
                                            drawDebugBounds(colorFront, strokeFront)
                                        }
                                    },
                                )
                            }
                        } ?: editorController.previewOverlayActor?.let { overlay ->
                            if (overlay is Visible) { // TODO: Handle else branch
                                withTransform(
                                    transformBlock = { overlay.transformForViewport(this) },
                                    drawBlock = {
                                        with(overlay) {
                                            clipRect(
                                                right = body.size.width.raw,
                                                bottom = body.size.height.raw
                                            ) {
                                                drawContext.canvas.alphaMultiplier = 0.4f
                                                draw()
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    },
                )
            }
        }
    }

    private fun DrawTransform.transformViewport(
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

    companion object {
        private const val HIGHLIGHT_BACKGROUND_ALPHA = 0.2f
        private const val HIGHLIGHT_BACKGROUND_FADE_SPEED = 0.002f
    }
}