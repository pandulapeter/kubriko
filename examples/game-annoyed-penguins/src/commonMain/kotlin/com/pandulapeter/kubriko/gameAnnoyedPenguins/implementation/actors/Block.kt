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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.DestructiblePhysicsObject
import com.pandulapeter.kubriko.types.SceneSize

internal class Block : DestructiblePhysicsObject() {

    override val body = RectangleBody(
        initialSize = SceneSize(
            width = 192.sceneUnit,
            height = 192.sceneUnit,
        )
    )

    override fun DrawScope.draw() {
        drawRect(
            color = Color.Cyan,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(width = 6f),
        )
    }
}