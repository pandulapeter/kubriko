/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal class GridManager : Manager() {

    val tileWidthMultiplier = MutableStateFlow(1f)
    val tileHeightMultiplier = MutableStateFlow(0.5f)
    private val viewportManager by manager<ViewportManager>()

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        val tileWidthMultiplierState = tileWidthMultiplier.collectAsState()
        val tileHeightMultiplierState = tileHeightMultiplier.collectAsState()
        val cameraPosition = viewportManager.cameraPosition.collectAsState()
        val colorScheme = MaterialTheme.colorScheme
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            drawIsometricGrid(
                color = colorScheme.onSurface.copy(alpha = 0.2f),
                tileWidth = tileWidthMultiplierState.value * GRID_SIZE,
                tileHeight = tileHeightMultiplierState.value * GRID_SIZE,
                cameraPosition = cameraPosition.value,
            )
        }
    }

    private fun DrawScope.drawIsometricGrid(
        color: Color,
        tileWidth: Float,
        tileHeight: Float,
        cameraPosition: SceneOffset,
    ) {
        fun positiveMod(value: Float, mod: Float): Float {
            val m = value % mod
            return if (m < 0) m + mod else m
        }

        val offsetX = positiveMod(cameraPosition.raw.x, tileWidth)
        val offsetY = positiveMod(cameraPosition.raw.y, tileHeight)

        val w = size.width
        val h = size.height
        val cx = w / 2f - offsetX
        val cy = h / 2f - offsetY

        val halfTileW = tileWidth / 2f
        val halfTileH = tileHeight / 2f
        val slopeRatio = halfTileW / halfTileH

        val strokeWidth = 1f

        // Big enough vertical span to always cover the screen
        val span = hypot(w, h) * 1.5f
        val halfSpan = span / 2f
        val lines = ceil(span / halfTileW).toInt() + 2

        // TL → BR
        for (i in -lines..lines) {
            val startX = cx + i * halfTileW - span * slopeRatio / 2f
            val startY = cy - halfSpan
            val endX = startX + span * slopeRatio
            val endY = cy + halfSpan
            drawLine(color, Offset(startX, startY), Offset(endX, endY), strokeWidth)
        }

        // TR → BL
        for (i in -lines..lines) {
            val startX = cx - i * halfTileW + span * slopeRatio / 2f
            val startY = cy - halfSpan
            val endX = startX - span * slopeRatio
            val endY = cy + halfSpan
            drawLine(color, Offset(startX, startY), Offset(endX, endY), strokeWidth)
        }
    }

    fun zoom(factor: Float) {
        if (factor <= 0f) return
        val curW = tileWidthMultiplier.value
        val curH = tileHeightMultiplier.value
        if (curW <= 0f || curH <= 0f) return
        val maxUp = min(ZOOM_MAXIMUM / curW, ZOOM_MAXIMUM / curH)
        val maxDown = max(ZOOM_MINIMUM / curW, ZOOM_MINIMUM / curH)
        val effectiveFactor = when {
            factor > 1f -> min(factor, maxUp)
            factor < 1f -> max(factor, maxDown)
            else -> 1f
        }
        tileWidthMultiplier.value = (curW * effectiveFactor).coerceIn(ZOOM_MINIMUM, ZOOM_MAXIMUM)
        tileHeightMultiplier.value = (curH * effectiveFactor).coerceIn(ZOOM_MINIMUM, ZOOM_MAXIMUM)
    }

    companion object {
        private const val GRID_SIZE = 256
        const val ZOOM_MINIMUM = 0.25f
        const val ZOOM_MAXIMUM = 2.25f
    }
}