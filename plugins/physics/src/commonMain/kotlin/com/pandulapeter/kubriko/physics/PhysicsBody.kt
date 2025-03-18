/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.helpers.extensions.cross
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.physics.implementation.geometry.Shape
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

// TODO: The shape should be set automatically based on the collisionMask
class PhysicsBody(
    var shape: Shape,
    var position: SceneOffset,
) {
    var dynamicFriction = 0.2f
    var staticFriction = 0.5f
    var orientation = AngleRadians.Zero
        set(value) {
            field = value
            shape.orientation.set(orientation)
            shape.createAABB()
        }
    var aabb = AxisAlignedBoundingBox(SceneOffset.Zero, SceneOffset.Zero)
    var velocity = SceneOffset.Zero
        internal set
    var force = SceneOffset.Zero
        internal set
    var angularVelocity = SceneUnit.Zero
    var torque = SceneUnit.Zero
    var restitution = 0.8f
    var density = 1f
        set(value) {
            field = value
            if (density == 0f) {
                setStatic()
            } else if (true) {
                shape.body = this
                shape.calcMass(value)
            }
        }
    var mass = 0f
    var invMass = 0f
    var inertia = 0f
    var invInertia = 0f
    var angularDampening = 0f
    var linearDampening = 0f
    var isAffectedByGravity = true
    var isParticle = false

    init {
        density = density
        shape.body = this
        shape.orientation.set(orientation)
        shape.createAABB()
    }

    /**
     * Applies force ot body.
     *
     * @param force        Force vector to apply.
     * @param contactPoint The point to apply the force to relative to the body in object space.
     */
    fun applyForce(force: SceneOffset, contactPoint: SceneOffset) {
        this.force += force
        torque += contactPoint.cross(force)
    }

    /**
     * Apply force to the center of mass.
     *
     * @param force Force vector to apply.
     */
    fun applyForce(force: SceneOffset) {
        this.force += force
    }

    /**
     * Applies impulse to a point relative to the body's center of mass.
     *
     * @param impulse      Magnitude of impulse vector.
     * @param contactPoint The point to apply the force to relative to the body in object space.
     */
    fun applyLinearImpulse(impulse: SceneOffset, contactPoint: SceneOffset) {
        velocity += impulse.scalar(invMass)
        angularVelocity += contactPoint.cross(impulse) * invInertia
    }

    /**
     * Applies impulse to body's center of mass.
     *
     * @param impulse Magnitude of impulse vector.
     */
    fun applyLinearImpulse(impulse: SceneOffset) {
        if (density > 0f) {
            velocity += impulse.scalar(invMass)
        }
    }

    /**
     * Sets all mass and inertia variables to zero. Object cannot be moved.
     */
    fun setStatic() {
        mass = 0f
        invMass = 0f
        inertia = 0f
        invInertia = 0f
    }
}