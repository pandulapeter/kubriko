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

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.helpers.TriangleBatch
import kotlin.math.abs

/**
 * A look's geometry in scene coordinates, rebuilt only when what it was built from changes: the body (its identity and
 * revision), the look's revision, the zoom, or whether a sprite is a stand-in or drawn on its own. [version] grows with
 * every rebuild, which is how a run of batched actors tells that it can draw last frame's arrays again.
 */
internal class LookGeometry {
    val batch = TriangleBatch()

    var version = 0
        private set

    /** The texture the geometry samples, or null for untextured geometry. */
    var texture: ImageBitmap? = null
        private set
    var tileMode = TileMode.Clamp
        private set
    var filterQuality = FilterQuality.Low
        private set

    /** Whether this is a sprite drawn on its own through `drawImageRect` rather than as geometry. */
    var isDrawnIndividually = false
        private set

    private val transform = LookTransform()
    private var builtBody: BoxBody? = null
    private var builtBodyRevision = 0
    private var builtLookRevision = 0
    private var builtPixelsPerUnit = Float.NaN
    private var builtIsStandIn = false
    private var builtIsDrawnIndividually = false

    fun update(look: Look, body: BoxBody, pixelsPerUnit: Float, isTextureSampledPerVertex: Boolean) {
        val bodyRevision = body.revision
        val lookRevision = look.revision
        val isStandIn = look is SpriteLook && isStandIn(look, body, pixelsPerUnit)
        isDrawnIndividually = look is SpriteLook && look.texture != null && !isStandIn && !isTextureSampledPerVertex
        updateRunKey(look, isStandIn)
        if (body === builtBody &&
            bodyRevision == builtBodyRevision &&
            lookRevision == builtLookRevision &&
            pixelsPerUnit == builtPixelsPerUnit &&
            isStandIn == builtIsStandIn &&
            isDrawnIndividually == builtIsDrawnIndividually
        ) return
        batch.reset()
        if (!isDrawnIndividually) {
            transform.setToBody(body)
            LookEmitter.emit(look, batch, body.size.width.raw, body.size.height.raw, transform, pixelsPerUnit, isStandIn)
        }
        builtBody = body
        builtBodyRevision = bodyRevision
        builtLookRevision = lookRevision
        builtPixelsPerUnit = pixelsPerUnit
        builtIsStandIn = isStandIn
        builtIsDrawnIndividually = isDrawnIndividually
        version++
    }

    private fun updateRunKey(look: Look, isStandIn: Boolean) {
        when (look) {
            is SpriteLook -> {
                texture = if (isStandIn) null else look.texture
                tileMode = TileMode.Clamp
                filterQuality = look.filterQuality
            }
            is CustomLook -> {
                texture = look.texture
                tileMode = look.textureTileMode
                filterQuality = look.textureFilterQuality
            }
            is RectangleLook, is EllipseLook -> {
                texture = null
                tileMode = TileMode.Clamp
                filterQuality = FilterQuality.Low
            }
        }
        if (texture == null) {
            tileMode = TileMode.Clamp
            filterQuality = FilterQuality.Low
        }
    }

    /** Whether two geometries can share one draw call: the same texture, sampled the same way. */
    fun canShareRunWith(other: LookGeometry) = texture === other.texture &&
        tileMode == other.tileMode &&
        filterQuality == other.filterQuality

    private fun isStandIn(look: SpriteLook, body: BoxBody, pixelsPerUnit: Float): Boolean {
        if (look.texture == null) return false
        val widthInPixels = abs(body.size.width.raw * body.scale.horizontal) * pixelsPerUnit
        val heightInPixels = abs(body.size.height.raw * body.scale.vertical) * pixelsPerUnit
        return widthInPixels < SpriteLook.STAND_IN_SIZE_IN_PIXELS && heightInPixels < SpriteLook.STAND_IN_SIZE_IN_PIXELS
    }
}
