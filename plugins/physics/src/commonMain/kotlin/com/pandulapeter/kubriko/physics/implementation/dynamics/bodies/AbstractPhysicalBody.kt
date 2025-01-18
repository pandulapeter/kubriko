/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics.implementation.dynamics.bodies

import com.pandulapeter.kubriko.physics.implementation.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.geometry.Polygon
import com.pandulapeter.kubriko.physics.implementation.math.Vec2

abstract class AbstractPhysicalBody : PhysicalBodyInterface {
    override var velocity = Vec2()
    override var force = Vec2()
    override var angularVelocity = 0f
    override var torque = 0f
    override var restitution = 0.8f
    override var density = 1f
        /**
         * Sets the density and calculates the mass depending on it.
         *
         * @param value the new value for density.
         */
        set(value) {
            field = value
            if (density == 0f) {
                setStatic()
            } else if (this is CollisionBodyInterface) {
                shape.body = this
                when (shape) {
                    is Circle -> (shape as Circle).calcMass(value)
                    is Polygon -> (shape as Polygon).calcMass(value)
                }
            } else {
                mass = density * 1000
                invMass = if (mass != 0f) 1f / mass else 0f
                inertia *= density
                invInertia = if (inertia != 0f) 1f / inertia else 0f
            }
        }
    override var mass = 0f
    override var invMass = 0f
    override var inertia = 0f
    override var invInertia = 0f
    override var angularDampening = 0f
    override var linearDampening = 0f
    override var affectedByGravity = true
    override var particle = false

    /**
     * Applies force ot body.
     *
     * @param force        Force vector to apply.
     * @param contactPoint The point to apply the force to relative to the body in object space.
     */
    override fun applyForce(force: Vec2, contactPoint: Vec2) {
        this.force.add(force)
        torque += contactPoint.cross(force)
    }

    /**
     * Apply force to the center of mass.
     *
     * @param force Force vector to apply.
     */
    override fun applyForce(force: Vec2) {
        this.force.add(force)
    }

    /**
     * Applies impulse to a point relative to the body's center of mass.
     *
     * @param impulse      Magnitude of impulse vector.
     * @param contactPoint The point to apply the force to relative to the body in object space.
     */
    override fun applyLinearImpulse(impulse: Vec2, contactPoint: Vec2) {
        velocity.add(impulse.scalar(invMass))
        angularVelocity += invInertia * contactPoint.cross(impulse)
    }

    /**
     * Applies impulse to body's center of mass.
     *
     * @param impulse Magnitude of impulse vector.
     */
    override fun applyLinearImpulse(impulse: Vec2) {
        velocity.add(impulse.scalar(invMass))
    }

    /**
     * Sets all mass and inertia variables to zero. Object cannot be moved.
     */
    override fun setStatic() {
        mass = 0f
        invMass = 0f
        inertia = 0f
        invInertia = 0f
    }
}