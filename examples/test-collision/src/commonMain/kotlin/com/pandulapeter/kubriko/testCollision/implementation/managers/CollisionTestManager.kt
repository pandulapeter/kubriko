/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testCollision.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.testCollision.implementation.actors.DraggableCollidableActor
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.random.Random

internal class CollisionTestManager : Manager() {

    private val actorManager by manager<ActorManager>()

    override fun onInitialize(kubriko: Kubriko) = actorManager.add(
        (0..ACTOR_COUNT).map {
            DraggableCollidableActor.newRandomShape(
                initialPosition = SceneOffset(
                    x = ((MINIMUM_DISTANCE_FROM_ORIGIN + AREA_LIMIT * Random.nextFloat()) * (if (Random.nextBoolean()) 1f else -1f)).sceneUnit,
                    y = ((MINIMUM_DISTANCE_FROM_ORIGIN + AREA_LIMIT * Random.nextFloat()) * (if (Random.nextBoolean()) 1f else -1f)).sceneUnit,
                )
            )
        }
    )

    companion object {
        private const val ACTOR_COUNT = 64
        const val AREA_LIMIT = 512
        private const val MINIMUM_DISTANCE_FROM_ORIGIN = 24
    }
}