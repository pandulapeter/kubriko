/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.looks

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.helpers.TriangleBatch
import com.pandulapeter.kubriko.helpers.TriangleBatchSupport
import com.pandulapeter.kubriko.implementation.pixelsPerLocalUnit

/** The one-off batch [drawLook] fills, reused across calls; drawing-thread only. */
private val directBatch = TriangleBatch()
private val identityTransform = LookTransform()

/**
 * Draws [look] over a [width] × [height] body into this [DrawScope], which is already transformed to the body: the
 * path for anything drawing a batched actor directly rather than through the layers. The canvas's own transform says
 * how many pixels a unit of the body covers, which sizes the edge smoothing and an ellipse's segments.
 */
internal fun DrawScope.drawLook(look: Look, width: Float, height: Float) {
    if (look is SpriteLook && TriangleBatchSupport.knownResult != true) {
        drawSpriteIndividually(look, width, height)
        return
    }
    val batch = directBatch
    batch.reset()
    identityTransform.setToIdentity()
    val canvas = drawContext.canvas
    LookEmitter.emit(look, batch, width, height, identityTransform, canvas.pixelsPerLocalUnit(), isStandIn = false)
    when (look) {
        is SpriteLook -> {
            batch.texture = look.texture
            batch.textureTileMode = TileMode.Clamp
            batch.textureFilterQuality = look.filterQuality
        }
        is CustomLook -> {
            batch.texture = look.texture
            batch.textureTileMode = look.textureTileMode
            batch.textureFilterQuality = look.textureFilterQuality
        }
        is RectangleLook, is EllipseLook -> batch.texture = null
    }
    batch.flush(canvas)
    batch.texture = null
}

/**
 * Draws a sprite through `drawImage`, stretched over a [width] × [height] body: for devices that cannot sample a
 * batch's texture per vertex, where Android's renderer still merges consecutive draws from the same image.
 */
internal fun DrawScope.drawSpriteIndividually(look: SpriteLook, width: Float, height: Float) {
    val texture = look.texture ?: return
    val regionWidth = look.regionWidth(texture)
    val regionHeight = look.regionHeight(texture)
    if (regionWidth <= 0 || regionHeight <= 0) return
    scale(scaleX = width / regionWidth, scaleY = height / regionHeight, pivot = Offset.Zero) {
        drawImage(
            image = texture,
            srcOffset = look.srcOffset,
            srcSize = IntSize(regionWidth, regionHeight),
            dstSize = IntSize(regionWidth, regionHeight),
            colorFilter = look.colorFilterForTint(),
            filterQuality = look.filterQuality,
        )
    }
}
