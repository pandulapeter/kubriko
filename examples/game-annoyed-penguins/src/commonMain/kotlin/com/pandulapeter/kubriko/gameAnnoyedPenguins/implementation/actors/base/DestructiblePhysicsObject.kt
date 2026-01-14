/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.mask.ComplexCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.Ground
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.Penguin
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.AudioManager
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.toSceneSize
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.sceneEditor.Editable
import kotlin.reflect.KClass

internal abstract class DestructiblePhysicsObject<T : DestructiblePhysicsObject<T>> : Visible, Editable<T>, Dynamic, RigidBody, CollisionDetector {

    abstract override val collisionMask: ComplexCollisionMask
    override val collidableTypes = listOf<KClass<out Collidable>>(Ground::class, Penguin::class, DestructiblePhysicsObject::class)
    private var timeSinceLastCrash = 0
    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: AudioManager
    private lateinit var viewportManager: ViewportManager
    private val lowestGroundY by lazy { actorManager.allActors.value.filterIsInstance<Ground>().maxOf { it.body.position.y } }

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        audioManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        if (timeSinceLastCrash >= 500 && physicsBody.velocity.length() > 20.sceneUnit) {
            timeSinceLastCrash = 0
            audioManager.playCrashSoundEffect()
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (deltaTimeInMilliseconds > 0) {
            if (body.position.y > viewportManager.bottomRight.value.y + viewportManager.size.value.toSceneSize(viewportManager).height && body.position.y > lowestGroundY) {
                actorManager.remove(this)
            } else {
                body.position = physicsBody.position
                body.rotation = physicsBody.rotation
                collisionMask.position = body.position
                (collisionMask as? PolygonCollisionMask)?.rotation = body.rotation
                if (timeSinceLastCrash < 500) {
                    timeSinceLastCrash += deltaTimeInMilliseconds
                }
            }
        }
    }
}