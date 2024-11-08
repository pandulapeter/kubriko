package com.pandulapeter.kubriko.physicsManager.implementation.physics.dynamics.bodies

import com.pandulapeter.kubriko.physicsManager.implementation.physics.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physicsManager.implementation.physics.math.Vec2

interface PhysicalBodyInterface :
    TranslatableBody {
    var velocity: Vec2
    var force: Vec2
    var angularVelocity: Double
    var torque: Double
    var restitution: Double
    var density: Double
    var mass: Double
    var invMass: Double
    var inertia: Double
    var invInertia: Double
    var angularDampening: Double
    var linearDampening: Double
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