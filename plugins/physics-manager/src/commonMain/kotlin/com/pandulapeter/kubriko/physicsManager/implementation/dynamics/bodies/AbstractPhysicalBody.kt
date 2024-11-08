package com.pandulapeter.kubriko.physicsManager.implementation.dynamics.bodies

import com.pandulapeter.kubriko.physicsManager.implementation.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.Circle
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.Polygon
import com.pandulapeter.kubriko.physicsManager.implementation.math.Vec2

abstract class AbstractPhysicalBody : PhysicalBodyInterface {
    override var velocity = Vec2()
    override var force = Vec2()
    override var angularVelocity = .0
    override var torque = .0
    override var restitution = .8
    override var density = 1.0
        /**
         * Sets the density and calculates the mass depending on it.
         *
         * @param value the new value for density.
         */
        set(value) {
            field = value
            if (density == .0) {
                setStatic()
            } else if (this is CollisionBodyInterface) {
                shape.body = this
                when (shape) {
                    is Circle -> (shape as Circle).calcMass(value)
                    is Polygon -> (shape as Polygon).calcMass(value)
                }
            } else {
                mass = density * 1000
                invMass = if (mass != 0.0) 1.0 / mass else 0.0
                inertia *= density
                invInertia = if (inertia != 0.0) 1.0 / inertia else 0.0
            }
        }
    override var mass = .0
    override var invMass = .0
    override var inertia = .0
    override var invInertia = .0
    override var angularDampening = .0
    override var linearDampening = .0
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
        mass = 0.0
        invMass = 0.0
        inertia = 0.0
        invInertia = 0.0
    }
}