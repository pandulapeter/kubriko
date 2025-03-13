/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class Brick(
    position: SceneOffset,
) : Visible, Collidable {
    override val body = BoxBody(
        initialPosition = position,
        initialSize = SceneSize(Width, Height),
    )
    var hue = randomHue()
        private set(value) {
            field = value
            color = createColor()
        }
    private var color = createColor()

    fun randomizeHue() {
        hue = randomHue()
    }

    private fun randomHue() = (0..360).random().toFloat()

    private fun createColor() = Color.hsv(hue, 0.3f, 1f)

    override fun DrawScope.draw() {
        drawRect(
            color = color,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(width = 3f),
        )
    }

    companion object {
        val Width = 100f.sceneUnit
        val Height = 40f.sceneUnit
    }
}
