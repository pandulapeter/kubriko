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
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class BrickPopEffect(
    position: SceneOffset,
    hue: Float,
) : Visible, Dynamic {
    override val body = RectangleBody(
        initialPosition = position,
        initialSize = SceneSize(Brick.Width, Brick.Height),
    )
    private val color = Color.hsv(hue, 0.2f, 0.9f)
    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager
    private var alpha = 1f

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (deltaTimeInMilliseconds > 0) {
            alpha -= 0.005f * deltaTimeInMilliseconds
            body.scale = Scale.Unit * alpha
            viewportManager.setScaleFactor(1f + (-10..10).random() * viewportManager.size.value.height * 0.000001f)
            if (alpha <= 0) {
                viewportManager.setScaleFactor(1f)
                actorManager.remove(this)
            }
        }
    }

    override fun DrawScope.draw() {
        drawRect(
            color = color.copy(alpha = alpha),
            size = body.axisAlignedBoundingBox.size.raw,
        )
        drawRect(
            color = Color.Black.copy(alpha = alpha),
            size = body.axisAlignedBoundingBox.size.raw,
            style = Stroke(),
        )
    }
}
