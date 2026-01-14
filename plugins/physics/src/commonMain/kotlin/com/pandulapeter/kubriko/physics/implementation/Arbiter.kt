/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.collision.extensions.collisionResultWith
import com.pandulapeter.kubriko.helpers.extensions.cross
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.abs

/**
 * Creates manifolds to detect collisions and apply forces to them. Discrete in nature and only evaluates pairs of bodies in a single manifold.
 */
internal data class Arbiter(
    val bodyA: PhysicsBody,
    val bodyB: PhysicsBody,
    private val penetrationCorrection: Float,
) {
    private var staticFriction = (bodyA.staticFriction + bodyB.staticFriction) / 2
    private var dynamicFriction = (bodyA.dynamicFriction + bodyB.dynamicFriction) / 2
    val contacts = arrayOf(SceneOffset.Zero, SceneOffset.Zero)
    var contactNormal = SceneOffset.Zero
    var isColliding = false
    var restitution = 0f
    private var penetration = SceneUnit.Zero

    fun narrowPhaseCheck() {
        restitution = bodyA.restitution.coerceAtMost(bodyB.restitution)
        bodyA.collisionMask.collisionResultWith(
            other = bodyB.collisionMask,
            shouldSkipAxisAlignedBoundingBoxCheck = true,
        )?.let { collisionResult ->
            isColliding = true
            contacts[0] = collisionResult.contact
            contactNormal = collisionResult.contactNormal
            penetration = collisionResult.penetration
        }
    }

    /**
     * Resolves any penetrations that are left overlapping between shapes. This can be cause due to integration errors of the solvers integration method.
     * Based on linear projection to move the shapes away from each other based on a correction constant and scaled relative to the inverse mass of the objects.
     */
    fun penetrationResolution() {
        val penetrationTolerance = penetration
        if (penetrationTolerance <= SceneUnit.Zero) {
            return
        }
        val totalMass = bodyA.mass + bodyB.mass
        val correction = penetrationTolerance * penetrationCorrection / totalMass
        bodyA.position = bodyA.position + contactNormal.scalar(-bodyA.mass.sceneUnit * correction)
        bodyB.position = bodyB.position + contactNormal.scalar(bodyB.mass.sceneUnit * correction)
    }

    /**
     * Solves the current contact manifold and applies impulses based on any contacts found.
     */
    fun solve() {
        val contactA = contacts[0] - bodyA.position
        val contactB = contacts[0] - bodyB.position

        //Relative velocity created from equation found in GDC talk of box2D lite.
        var relativeVel = bodyB.velocity
            .plus(contactB.cross(bodyB.angularVelocity.raw))
            .minus(bodyA.velocity)
            .minus(contactA.cross(bodyA.angularVelocity.raw))

        //Positive = converging Negative = diverging
        val contactVel = relativeVel.dot(contactNormal)

        //Prevents objects colliding when they are moving away from each other.
        //If not, objects could still be overlapping after a contact has been resolved and cause objects to stick together
        if (contactVel >= SceneUnit.Zero) {
            return
        }
        val acn = contactA.cross(contactNormal).raw
        val bcn = contactB.cross(contactNormal).raw
        val inverseMassSum = bodyA.invMass + bodyB.invMass + acn * acn * bodyA.invInertia + bcn * bcn * bodyB.invInertia
        var j = -(restitution.sceneUnit + SceneUnit.Unit) * contactVel
        j /= inverseMassSum
        val impulse = contactNormal.scalar(j)
        bodyB.applyLinearImpulse(impulse, contactB)
        bodyA.applyLinearImpulse(-impulse, contactA)
        relativeVel = bodyB.velocity
            .plus(contactB.cross(bodyB.angularVelocity.raw))
            .minus(bodyA.velocity)
            .minus(contactA.cross(bodyA.angularVelocity.raw))
        val t = (relativeVel + contactNormal.scalar(-relativeVel.dot(contactNormal))).normalized()
        var jt = -relativeVel.dot(t)
        jt /= inverseMassSum
        val tangentImpulse = if (abs(jt.raw).sceneUnit < j * staticFriction) {
            t.scalar(jt)
        } else {
            t.scalar(j).scalar(-dynamicFriction)
        }
        bodyB.applyLinearImpulse(tangentImpulse, contactB)
        bodyA.applyLinearImpulse(-tangentImpulse, contactA)
    }
}