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

import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.abs
import com.pandulapeter.kubriko.extensions.angleTowards
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.distanceTo
import com.pandulapeter.kubriko.extensions.lerp
import com.pandulapeter.kubriko.extensions.min
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.BlinkingPenguin
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

internal class FakePenguin(
    private val initialPosition: SceneOffset,
) : BlinkingPenguin(
    body = CircleBody(
        initialPosition = initialPosition,
        initialRadius = 128.sceneUnit,
        initialScale = Scale.Zero,
    ),
), Unique, Dynamic {
    override var isVisible = false
        set(value) {
            if (field != value) {
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
    var adjustedTargetPosition: SceneOffset = initialPosition
        private set

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
        private val maximumRadius = 750.sceneUnit
    }
}