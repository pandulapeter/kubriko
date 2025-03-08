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

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.toSceneSize
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.BlinkingPenguin
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.math.Vec2
import com.pandulapeter.kubriko.types.SceneOffset

internal class Penguin(
    initialPosition: SceneOffset,
    impulseOrigin: SceneOffset,
) : BlinkingPenguin(initialPosition), RigidBody {

    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager
    override val physicsBody = Body(
        shape = Circle(
            radius = body.radius * 0.8f,
        ),
        x = body.position.x,
        y = body.position.y * 1.2f,
    ).apply {
        restitution = 0.5f
        angularDampening = 1000f
        orientation = body.rotation
        applyForce(
            Vec2(
                x = impulseOrigin.x,
                y = impulseOrigin.y,
            ).scalar(500000f)
        )
    }

    override fun onAdded(kubriko: Kubriko) {
        super<BlinkingPenguin>.onAdded(kubriko)
        actorManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        super.update(deltaTimeInMilliseconds)
        body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
        body.rotation = physicsBody.orientation
        if (body.position.y > viewportManager.bottomRight.value.y + viewportManager.size.value.toSceneSize(viewportManager).height) {
            actorManager.remove(this)
        }
    }
}