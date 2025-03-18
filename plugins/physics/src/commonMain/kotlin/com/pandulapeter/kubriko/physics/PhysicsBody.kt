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
import com.pandulapeter.kubriko.collision.implementation.Vec2
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.physics.implementation.geometry.Shape
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset

// TODO: The shape should be set automatically based on the collisionMask
class PhysicsBody(
    var shape: Shape,
    var position: SceneOffset,
) {
    var dynamicFriction = 0.2f
    var staticFriction = 0.5f
    var orientation = AngleRadians.Companion.Zero
        set(value) {
            field = value
            shape.orientation.set(orientation)
            shape.createAABB()
        }
    var aabb = AxisAlignedBoundingBox(SceneOffset.Companion.Zero, SceneOffset.Companion.Zero)
    var velocity = Vec2()
    var force = Vec2()
    var angularVelocity = 0f
    var torque = 0f
    var restitution = 0.8f
    var density = 1f
        set(value) {
            field = value
            if (density == 0f) {
                setStatic()
            } else if (true) {
                shape.body = this
                when (shape) {
                    is Circle -> (shape as Circle).calcMass(value)
                    is Polygon -> (shape as Polygon).calcMass(value)
                }
            }
        }
    var mass = 0f
    var invMass = 0f
    var inertia = 0f
    var invInertia = 0f
    var angularDampening = 0f
    var linearDampening = 0f
    var affectedByGravity = true
    var particle = false

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
    fun applyForce(force: Vec2, contactPoint: Vec2) {
        this.force.add(force)
        torque += contactPoint.cross(force)
    }

    /**
     * Apply force to the center of mass.
     *
     * @param force Force vector to apply.
     */
    fun applyForce(force: Vec2) {
        this.force.add(force)
    }

    /**
     * Applies impulse to a point relative to the body's center of mass.
     *
     * @param impulse      Magnitude of impulse vector.
     * @param contactPoint The point to apply the force to relative to the body in object space.
     */
    fun applyLinearImpulse(impulse: Vec2, contactPoint: Vec2) {
        velocity.add(impulse.scalar(invMass))
        angularVelocity += invInertia * contactPoint.cross(impulse)
    }

    /**
     * Applies impulse to body's center of mass.
     *
     * @param impulse Magnitude of impulse vector.
     */
    fun applyLinearImpulse(impulse: Vec2) {
        if (density > 0f) {
            velocity.add(impulse.scalar(invMass))
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