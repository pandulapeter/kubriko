/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu.implementation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.minus
import com.pandulapeter.kubriko.helpers.extensions.transformForViewport
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class DebugMenuManager(
    private val gameKubriko: Kubriko,
) : Manager(), Overlay, Unique {
    private val gameActorManager by lazy { gameKubriko.get<ActorManager>() }
    private val gameStateManager by lazy { gameKubriko.get<StateManager>() }
    private val gameMetadataManager by lazy { gameKubriko.get<MetadataManager>() }
    private val gameViewportManager by lazy { gameKubriko.get<ViewportManager>() }
    private val debugColor = Color.Cyan
    override val overlayDrawingOrder = Float.MIN_VALUE

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().let {
            it.removeAll()
            it.add(this)
        }
        combine(
            gameMetadataManager.fps,
            combine(gameActorManager.allActors, gameActorManager.visibleActorsWithinViewport) { all, visible -> all to visible },
            gameMetadataManager.activeRuntimeInMilliseconds,
            gameViewportManager.size,
            InternalDebugMenu.isDebugOverlayEnabled,
        ) { fps, (allActors, visibleActorsWithinViewport), runtimeInMilliseconds, viewportSize, isDebugOverlayEnabled ->
            DebugMenuMetadata(
                kubrikoInstanceName = gameKubriko.instanceName,
                fps = fps,
                totalActorCount = allActors.count(),
                visibleActorWithinViewportCount = visibleActorsWithinViewport.count(),
                playTimeInSeconds = runtimeInMilliseconds / 1000,
                viewportSize = viewportSize,
                isDebugOverlayEnabled = isDebugOverlayEnabled,
            )
        }.onEach {
            if (gameStateManager.isFocused.value) {
                InternalDebugMenu.setMetadata(it)
            }
        }.launchIn(scope)
    }

    override fun DrawScope.drawToViewport() {
        if (InternalDebugMenu.isDebugOverlayEnabled.value) {
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
                            val stroke = Stroke(
                                width = 4f / (scaleFactor.horizontal + scaleFactor.vertical),
                                join = StrokeJoin.Round,
                            )
                            gameActorManager.visibleActorsWithinViewport.value.forEach { visible ->
                                drawRect(
                                    color = debugColor,
                                    topLeft = visible.body.axisAlignedBoundingBox.min.raw,
                                    size = visible.body.axisAlignedBoundingBox.size.raw,
                                    style = stroke,
                                )
                                val body = (visible as? Collidable)?.collisionMask ?: visible.body
                                withTransform(
                                    transformBlock = { body.transformForViewport(this) },
                                    drawBlock = {
                                        with(body) {
                                            drawDebugBounds(debugColor, stroke)
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
}