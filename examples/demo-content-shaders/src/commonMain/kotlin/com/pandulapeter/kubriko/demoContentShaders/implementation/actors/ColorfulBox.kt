/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class ColorfulBox(
    initialPosition: SceneOffset,
    private var hue: Float,
    private val shouldDrawBorder: () -> Boolean,
) : Visible, Dynamic {

    override val body = BoxBody(
        initialPosition = initialPosition,
        initialSize = SceneSize(width = 100.sceneUnit, height = 100.sceneUnit),
    )

    override fun update(deltaTimeInMilliseconds: Int) {
        hue = (hue + deltaTimeInMilliseconds / 10f) % 360f
    }

    override fun DrawScope.draw() {
        drawRect(
            color = Color.hsv(hue, 0.5f, 1f),
            size = body.size.raw,
        )
        if (shouldDrawBorder()) {
            drawRect(
                color = Color.Black,
                size = body.size.raw,
                style = Stroke()
            )
        }
    }
}