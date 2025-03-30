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
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.BlinkingPenguin
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.DestructiblePhysicsObject
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.AudioManager
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.toSceneSize
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.reflect.KClass

internal class Penguin(
    initialPosition: SceneOffset,
    impulseOrigin: SceneOffset,
) : BlinkingPenguin(initialPosition), RigidBody, CollisionDetector {

    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: AudioManager
    private lateinit var viewportManager: ViewportManager
    val radius = body.size.width * 0.4f
    override val collidableTypes = listOf<KClass<out Collidable>>(Ground::class, DestructiblePhysicsObject::class, Penguin::class)
    override val collisionMask = CircleCollisionMask(
        initialRadius = radius,
        initialPosition = body.position,
    )
    override val physicsBody = PhysicsBody(collisionMask).apply {
        restitution = 0.1f
        density = 5f
        rotation = body.rotation
        applyForce(impulseOrigin.scalar(1500000f))
    }
    override val drawingOrder = -2f
    var shouldBeFollowedByCamera = true
    private var timeSinceLastCollision = -2000

    override fun onAdded(kubriko: Kubriko) {
        super<BlinkingPenguin>.onAdded(kubriko)
        actorManager = kubriko.get()
        audioManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        if (physicsBody.velocity.length() > 50.sceneUnit) {
            audioManager.playPopSoundEffect()
            timeSinceLastCollision = 0
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        super.update(deltaTimeInMilliseconds)
        body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
        // body.rotation = physicsBody.orientation
        if (body.position.y > viewportManager.bottomRight.value.y + viewportManager.size.value.toSceneSize(viewportManager).height && !shouldBeFollowedByCamera) {
            actorManager.remove(this)
        } else {
            collisionMask.position = body.position
        }
        if (shouldBeFollowedByCamera) {
            timeSinceLastCollision += deltaTimeInMilliseconds
            if (timeSinceLastCollision >= 2000) {
                shouldBeFollowedByCamera = false
            }
        }
    }
}