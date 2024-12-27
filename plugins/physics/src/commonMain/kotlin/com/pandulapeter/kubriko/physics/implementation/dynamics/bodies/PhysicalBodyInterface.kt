package com.pandulapeter.kubriko.physics.implementation.dynamics.bodies

import com.pandulapeter.kubriko.physics.implementation.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physics.implementation.math.Vec2

interface PhysicalBodyInterface :
    TranslatableBody {
    var velocity: Vec2
    var force: Vec2
    var angularVelocity: Float
    var torque: Float
    var restitution: Float
    var density: Float
    var mass: Float
    var invMass: Float
    var inertia: Float
    var invInertia: Float
    var angularDampening: Float
    var linearDampening: Float
    var affectedByGravity: Boolean
    var particle: Boolean

    /**
     * Applies force ot body.
     *
     * @param force        Force vector to apply.
     * @param contactPoint The point to apply the force to relative to the body in object space.
     */
    fun applyForce(force: Vec2, contactPoint: Vec2)

    /**
     * Apply force to the center of mass.
     *
     * @param force Force vector to apply.
     */
    fun applyForce(force: Vec2)

    /**
     * Applies impulse to a point relative to the body's center of mass.
     *
     * @param impulse      Magnitude of impulse vector.
     * @param contactPoint The point to apply the force to relative to the body in object space.
     */
    fun applyLinearImpulse(impulse: Vec2, contactPoint: Vec2)

    /**
     * Applies impulse to body's center of mass.
     *
     * @param impulse Magnitude of impulse vector.
     */
    fun applyLinearImpulse(impulse: Vec2)

    /**
     * Sets all mass and inertia variables to zero. Object cannot be moved.
     */
    fun setStatic()
}