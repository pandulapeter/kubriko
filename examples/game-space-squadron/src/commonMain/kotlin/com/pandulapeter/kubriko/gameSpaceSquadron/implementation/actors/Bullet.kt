/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset

internal class Bullet(
    startingPosition: SceneOffset,
) : Visible, Dynamic {
    override val body = CircleBody(
        initialRadius = 8.sceneUnit,
        initialPosition = startingPosition,
    )
    override val drawingOrder = 1f
    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        viewportManager = kubriko.get()
        kubriko.get<AudioManager>().playShootSoundEffect()
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        body.position -= SceneOffset.Down * deltaTimeInMilliseconds
        if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
            actorManager.remove(this)
        }
    }

    override fun DrawScope.draw() = drawCircle(
        color = Color.White,
        radius = body.radius.raw,
        center = body.size.center.raw,
    )
}