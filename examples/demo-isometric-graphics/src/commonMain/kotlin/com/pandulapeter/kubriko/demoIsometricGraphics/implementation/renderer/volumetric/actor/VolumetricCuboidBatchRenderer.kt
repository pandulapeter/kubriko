/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.actor

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.TriangleBatch

// Draws every tracked cuboid in a single actor: VolumetricRenderManager keeps [renderers] sorted
// by depth, and this actor walks the list back to front, emitting solid faces and outlines into a
// shared TriangleBatch (a handful of drawVertices calls per frame) while textured faces draw
// through the regular image path. Replaces one-actor-per-cuboid rendering, whose per-face canvas
// operations dominated the frame cost.
class VolumetricCuboidBatchRenderer(
    private val renderers: () -> List<VolumetricCuboidRenderer>,
) : Visible {

    // The body exists only to keep this actor inside the engine's viewport culling no matter where
    // the camera is; per-cuboid culling happens in draw(). BOUNDS is a power of two so the engine's
    // top-left translation and the pivot compensation below cancel exactly in float arithmetic.
    override val body = BoxBody(
        initialPosition = SceneOffset.Zero,
        initialSize = SceneSize((2 * BOUNDS).sceneUnit, (2 * BOUNDS).sceneUnit),
        initialPivot = SceneOffset(BOUNDS.sceneUnit, BOUNDS.sceneUnit),
    )
    override val shouldClip = false

    private val batch = TriangleBatch()
    private var viewportManager: ViewportManager? = null

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }

    override fun DrawScope.draw() {
        val viewportManager = viewportManager ?: return
        val currentRenderers = renderers()
        if (currentRenderers.isEmpty()) return
        val topLeft = viewportManager.topLeft.value
        val bottomRight = viewportManager.bottomRight.value
        val leftBound = topLeft.x.raw
        val topBound = topLeft.y.raw
        val rightBound = bottomRight.x.raw
        val bottomBound = bottomRight.y.raw
        val pixelsPerUnit = viewportManager.scaleFactor.value.horizontal
        val canvas = drawContext.canvas
        canvas.save()
        // The engine translated the canvas to this actor's AABB corner; translate back so all
        // emission happens in plain scene coordinates.
        canvas.translate(BOUNDS, BOUNDS)
        for (i in currentRenderers.indices) {
            val renderer = currentRenderers[i]
            val rendererBody = renderer.body
            val rendererLeft = rendererBody.position.x.raw - rendererBody.pivot.x.raw
            val rendererTop = rendererBody.position.y.raw - rendererBody.pivot.y.raw
            if (rendererLeft <= rightBound &&
                rendererTop <= bottomBound &&
                rendererLeft + rendererBody.size.width.raw >= leftBound &&
                rendererTop + rendererBody.size.height.raw >= topBound
            ) {
                with(renderer) { emitBatched(batch, pixelsPerUnit) }
            }
        }
        batch.flush(canvas)
        canvas.restore()
    }

    private companion object {
        const val BOUNDS = 1048576f // 2^20
    }
}
