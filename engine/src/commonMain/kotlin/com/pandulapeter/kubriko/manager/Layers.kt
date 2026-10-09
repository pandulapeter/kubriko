/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.helpers.extensions.minus
import com.pandulapeter.kubriko.helpers.extensions.transformForViewport
import com.pandulapeter.kubriko.helpers.extensions.transformViewport
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

/**
 * A scope of its own, so that a layer coming or going recomposes only the layers rather than rebuilding the
 * container's whole modifier chain (restarting its pointer handlers among others), and keyed by index, so that
 * one appearing in front of the others doesn't hand every later layer's node to a different layer.
 */
@Composable
internal fun Layers(
    layerIndices: StateFlow<ImmutableList<Int?>>,
    gameTime: State<Long>,
    managers: List<Manager>,
    viewportManager: ViewportManagerImpl,
    culler: ActorCuller,
) {
    val layers = layerIndices.collectAsState().value
    layers.forEach { layerIndex ->
        key(layerIndex) {
            Layer(
                gameTime = gameTime,
                layerIndex = layerIndex,
                managers = managers,
                viewportManager = viewportManager,
                culler = culler,
            )
        }
    }
}

@Composable
private fun Layer(
    gameTime: State<Long>,
    layerIndex: Int?,
    managers: List<Manager>,
    viewportManager: ViewportManagerImpl,
    culler: ActorCuller,
) {
    Canvas(
        modifier = if (layerIndex == null) {
            Modifier.fillMaxSize().clipToBounds()
        } else {
            managers.fold(Modifier.fillMaxSize().clipToBounds()) { modifierToProcess, manager ->
                manager.processModifierInternal(modifierToProcess, layerIndex, gameTime)
            }
        },
        onDraw = {
            @Suppress("UNUSED_EXPRESSION") gameTime.value
            val visibles = culler.sortedVisibleActorsByLayer[layerIndex]
            if (!visibles.isNullOrEmpty()) {
                val viewportCenter = viewportManager.cameraPosition.value
                val viewportSize = viewportManager.size.value
                val scaleFactor = viewportManager.currentScaleFactor()
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportCenter = viewportCenter,
                            shiftedViewportOffset = (viewportSize / 2f) - viewportCenter,
                            viewportScaleFactor = scaleFactor,
                        )
                    },
                    drawBlock = {
                        val canvas = drawContext.canvas
                        val transform = drawContext.transform
                        for (i in visibles.indices) {
                            val visible = visibles[i]
                            if (visible.isVisible) {
                                canvas.save()
                                visible.body.transformForViewport(transform)
                                with(visible) {
                                    if (shouldClip) {
                                        transform.clipRect(
                                            left = 0f,
                                            top = 0f,
                                            right = body.size.width.raw,
                                            bottom = body.size.height.raw,
                                        )
                                    }
                                    draw()
                                }
                                canvas.restore()
                            }
                        }
                    },
                )
            }
            val overlays = culler.sortedOverlayActorsByLayer[layerIndex]
            if (!overlays.isNullOrEmpty()) {
                for (i in overlays.indices) {
                    with(overlays[i]) { drawToViewport() }
                }
            }
        }
    )
}
