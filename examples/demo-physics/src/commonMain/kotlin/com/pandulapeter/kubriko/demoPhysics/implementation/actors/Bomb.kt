/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import com.pandulapeter.kubriko.physics.explosions.ProximityExplosion
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.max
import kotlin.math.min

internal class Bomb(
    epicenter: SceneOffset,
) : Visible, Dynamic {

    override val body = BoxBody(
        initialSize = SceneSize(10.sceneUnit, 10.sceneUnit),
        initialPosition = epicenter,
    )
    private lateinit var actorManager: ActorManager
    private var remainingLifetimeInMilliseconds = LIFETIME_IN_MILLISECONDS
    private val explosion = ProximityExplosion(
        epicenter = epicenter,
        proximity = 750.sceneUnit,
    )

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        explosion.update(actorManager.allActors.value.filterIsInstance<RigidBody>().map { it.physicsBody })
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        body.size += SceneSize(5.sceneUnit, 5.sceneUnit) * deltaTimeInMilliseconds
        body.pivot = body.size.center
        val activeTimeInMilliseconds = min(deltaTimeInMilliseconds, remainingLifetimeInMilliseconds)
        remainingLifetimeInMilliseconds -= deltaTimeInMilliseconds
        if (activeTimeInMilliseconds > 0) {
            explosion.applyBlastImpulse((BLAST_POWER_PER_MILLISECOND * activeTimeInMilliseconds).sceneUnit)
        }
        if (remainingLifetimeInMilliseconds <= 0) {
            actorManager.remove(this)
        }
    }

    override fun DrawScope.draw() = drawCircle(
        color = Color.White.copy(alpha = max(0f, remainingLifetimeInMilliseconds / LIFETIME_IN_MILLISECONDS.toFloat())),
        radius = body.size.width.raw / 2f,
        center = body.size.center.raw,
    )

    companion object {
        private const val LIFETIME_IN_MILLISECONDS = 100
        private const val BLAST_POWER_PER_MILLISECOND = 1_500_000f
    }
}