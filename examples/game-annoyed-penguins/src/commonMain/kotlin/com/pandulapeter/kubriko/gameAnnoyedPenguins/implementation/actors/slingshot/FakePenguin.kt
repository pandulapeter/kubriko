/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.slingshot

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.Penguin
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.BlinkingPenguin
import com.pandulapeter.kubriko.helpers.extensions.abs
import com.pandulapeter.kubriko.helpers.extensions.angleTowards
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.lerp
import com.pandulapeter.kubriko.helpers.extensions.min
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

internal class FakePenguin(
    private val initialPosition: SceneOffset,
) : BlinkingPenguin(initialPosition), Unique, Dynamic {

    private lateinit var actorManager: ActorManager
    override var isVisible = false
        set(value) {
            if (field != value) {
                if (!value) {
                    actorManager.add(
                        Penguin(
                            initialPosition = body.position,
                            impulseOrigin = initialPosition - body.position,
                        )
                    )
                }
                body.position = initialPosition
                distanceFromTarget = 1f
                pointerPosition = initialPosition
                field = value
            }
        }
    var distanceFromTarget = 1f
        private set(value) {
            field = value
            body.scale = Scale.Unit * (1f - value)
        }
    var pointerPosition: SceneOffset = initialPosition
    private var adjustedTargetPosition: SceneOffset = initialPosition

    init {
        body.scale = Scale.Zero
    }

    override fun onAdded(kubriko: Kubriko) {
        super<BlinkingPenguin>.onAdded(kubriko)
        actorManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isVisible) {
            super.update(deltaTimeInMilliseconds)
            val angle = -initialPosition.angleTowards(pointerPosition)
            val distance = min(abs(initialPosition.distanceTo(pointerPosition)), maximumRadius)
            adjustedTargetPosition = initialPosition + SceneOffset(
                x = distance * angle.cos,
                y = -distance * angle.sin,
            )
            body.position = if (distanceFromTarget <= 0f) {
                adjustedTargetPosition
            } else {
                distanceFromTarget -= deltaTimeInMilliseconds * 0.005f
                lerp(adjustedTargetPosition, body.position, distanceFromTarget)
            }
        }
    }

    companion object {
        val maximumRadius = 650.sceneUnit
    }
}