/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.abs
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.toSceneSize
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_star

internal class StarIndicator(
    private val star: Star,
) : Visible, Dynamic {

    override val body = star.body.copyAsBoxBody()
    private lateinit var actorManager: ActorManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var viewportManager: ViewportManager
    private val paint = Paint().apply { alpha = 0.5f }
    override val drawingOrder = -10f

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        spriteManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun DrawScope.draw() {
        spriteManager.get(Res.drawable.sprite_star)?.let { image ->
            drawIntoCanvas { canvas ->
                canvas.drawImage(
                    image = image,
                    topLeftOffset = Offset.Zero,
                    paint = paint,
                )
            }
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        body.rotation = star.body.rotation
        body.scale = star.body.scale
        body.position = calculateIndicatorPosition(
            starPosition = star.body.position,
            viewportCenter = viewportManager.cameraPosition.value,
            viewportSize = viewportManager.size.value.toSceneSize(viewportManager),
        )
        if (star !in actorManager.allActors.value) {
            actorManager.remove(this)
        }
    }

    private fun calculateIndicatorPosition(
        starPosition: SceneOffset,
        viewportCenter: SceneOffset,
        viewportSize: SceneSize
    ): SceneOffset {
        val halfWidth = viewportSize.width
        val halfHeight = viewportSize.height
        val direction = starPosition - viewportCenter
        if (abs(direction.x) <= halfWidth && abs(direction.y) <= halfHeight) {
            return starPosition
        }
        val aspectRatio = direction.y / direction.x
        val xBound = if (direction.x > SceneUnit.Zero) halfWidth else -halfWidth
        val yAtXBound = xBound * aspectRatio
        val yBound = if (direction.y > SceneUnit.Zero) halfHeight else -halfHeight
        val xAtYBound = yBound / aspectRatio
        val clampedPosition = if (abs(yAtXBound) <= halfHeight) {
            viewportCenter + SceneOffset(xBound, yAtXBound)
        } else {
            viewportCenter + SceneOffset(xAtYBound, yBound)
        }
        return clampedPosition
    }
}