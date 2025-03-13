/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.explosions.ProximityExplosion
import com.pandulapeter.kubriko.physics.implementation.math.Vec2
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class Bomb(
    epicenter: SceneOffset,
) : Visible, Dynamic {

    override val body = BoxBody(
        initialSize = SceneSize(10.sceneUnit, 10.sceneUnit),
        initialPosition = epicenter,
    )
    private lateinit var actorManager: ActorManager
    private var alpha = 1f
    private val explosion = ProximityExplosion(
        epicentre = Vec2(
            x = epicenter.x,
            y = epicenter.y,
        ),
        proximity = 750.sceneUnit,
    )

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        explosion.update(actorManager.allActors.value.filterIsInstance<RigidBody>().map { it.physicsBody })
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        body.size += SceneSize(5.sceneUnit, 5.sceneUnit) * deltaTimeInMilliseconds
        body.pivot = body.size.center
        alpha -= 0.01f * deltaTimeInMilliseconds
        if (alpha <= 0) {
            actorManager.remove(this)
        } else {
            explosion.applyBlastImpulse(25000000.sceneUnit)
        }
    }

    override fun DrawScope.draw() = drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = body.size.width.raw / 2f,
        center = body.size.center.raw,
    )
}