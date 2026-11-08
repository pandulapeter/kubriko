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

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.actor.looks.drawSpriteIndividually
import com.pandulapeter.kubriko.actor.traits.Batched
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.TriangleBatch
import com.pandulapeter.kubriko.helpers.extensions.transformForViewport

/** Draws what [BatchedLayerDrawer] hands it into the layer's [DrawScope], already transformed to the viewport. */
internal class CanvasLayerTarget : BatchedLayerTarget {
    var drawScope: DrawScope? = null

    override fun drawPlain(visible: Visible) = with(drawScope!!) {
        val canvas = drawContext.canvas
        val transform = drawContext.transform
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

    override fun drawRun(batch: TriangleBatch) = batch.draw(drawScope!!.drawContext.canvas)

    override fun drawSpriteIndividually(actor: Batched, look: SpriteLook) = with(drawScope!!) {
        val canvas = drawContext.canvas
        canvas.save()
        actor.body.transformForViewport(drawContext.transform)
        drawSpriteIndividually(look, actor.body.size.width.raw, actor.body.size.height.raw)
        canvas.restore()
    }
}
