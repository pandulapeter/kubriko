/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * This penguin is "loaded" into the slingshot. It handles the aiming logic and instantiating the real physics-aware Penguin.
 */
internal class ActiveFakePenguin(
    private val initialPosition: SceneOffset,
    private val waitingFakePenguin: WaitingFakePenguin,
) : BlinkingPenguin(waitingFakePenguin.body.position), Unique, Dynamic {

    private lateinit var actorManager: ActorManager
    override var isVisible = false
        set(value) {
            if (field != value) {
                if (value) {
                    waitingFakePenguin.reset()
                } else {
                    actorManager.add(
                        Penguin(
                            initialPosition = body.position,
                            impulseOrigin = initialPosition - body.position,
                        )
                    )
                }
                body.position = waitingFakePenguin.body.position
                distanceFromTarget = 1f
                pointerPosition = initialPosition
                field = value
            }
        }
    var distanceFromTarget = 1f
    var pointerPosition: SceneOffset = initialPosition
    override val drawingOrder get() = if (distanceFromTarget <= 0.8f) 0f else -2f
    private var adjustedTargetPosition: SceneOffset = initialPosition

    override fun onAdded(kubriko: Kubriko) {
        super<BlinkingPenguin>.onAdded(kubriko)
        actorManager = kubriko.get()
    }

    private fun downwardFactor(angle: AngleRadians): Float {
        val downwardAngle = AngleRadians.HalfPi * 3
        val range = AngleRadians.Pi / 4
        return 1 - when {
            angle.normalized in (downwardAngle - range).normalized..(downwardAngle + range).normalized -> {
                val t = ((angle - downwardAngle) * (AngleRadians.Pi.normalized / (range.normalized * 2f))).cos.coerceIn(0f, 0.75f)
                val smoothT = t * t * (3 - 2 * t)
                (smoothT * 0.75f).coerceIn(0f, 0.75f)
            }

            else -> 0f
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isVisible) {
            super.update(deltaTimeInMilliseconds)
            val angle = -initialPosition.angleTowards(pointerPosition)
            val distance = min(abs(initialPosition.distanceTo(pointerPosition)), maximumRadius * downwardFactor(angle))
            adjustedTargetPosition = initialPosition + SceneOffset(
                x = distance * angle.cos,
                y = -distance * angle.sin,
            )
            body.position = if (distanceFromTarget <= 0f) {
                adjustedTargetPosition
            } else {
                distanceFromTarget -= deltaTimeInMilliseconds * 0.001f
                lerp(adjustedTargetPosition, body.position, distanceFromTarget)
            }
        }
    }

    companion object {
        val maximumRadius = 650.sceneUnit
    }
}