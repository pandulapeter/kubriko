package com.pandulapeter.kubriko.physics.implementation.physics.dynamics

import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.implementation.physics.collision.Arbiter
import com.pandulapeter.kubriko.physics.implementation.physics.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.physics.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physics.implementation.physics.joints.Joint
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import kotlin.math.pow

/**
 * Class for creating a world with iterative solver structure.
 *
 * @param gravity The strength of gravity in the world.
 */
class World(
    private var gravity: Vec2 = Vec2(),
    private val getRigidBodies: () -> List<TranslatableBody>,
    private val getJoints: () -> List<Joint>,
) {

    /**
     * Adds a body to the world
     *
     * @param b Body to add.
     * @return Returns the newly added body.
     */
    fun addBody(b: TranslatableBody): TranslatableBody {
        // TODO
        return b
    }

    /**
     * Removes a body from the world.
     *
     * @param b The body to remove from the world.
     */
    fun removeBody(b: TranslatableBody) {
        // TODO
    }

    /**
     * Adds a joint to the world.
     *
     * @param j The joint to add.
     * @return Returns the joint added to the world.
     */
    fun addJoint(j: Joint): Joint {
        // TODO
        return j
    }

    /**
     * Removes a joint from the world.
     *
     * @param j The joint to remove from the world.
     */
    fun removeJoint(j: Joint) {
        // TODO
    }

    var contacts = ArrayList<Arbiter>()

    /**
     * The main time step method for the world to conduct an iteration of the current world call this method with a desired time step value.
     *
     * @param dt Timestep
     */
    fun step(dt: Float) {
        contacts.clear()
        broadPhaseCheck()
        semiImplicit(dt)

        //Correct positional errors from the discrete collisions
        for (contact in contacts) {
            contact.penetrationResolution()
        }
    }

    /**
     * Semi implicit euler integration method for the world bodies and forces.
     *
     * @param dt Timestep
     */
    private fun semiImplicit(dt: Float) {
        //Applies tentative velocities
        applyForces(dt)
        solve()

        //Integrate positions
        for (b in getRigidBodies()) {
            if (b !is PhysicalBodyInterface) continue
            if (b.invMass == 0f) {
                continue
            }
            b.position.add(b.velocity.scalar(dt))
            if (b is CollisionBodyInterface) {
                b.orientation += (dt * b.angularVelocity).rad
            }
            b.force.set(0f.sceneUnit, 0f.sceneUnit)
            b.torque = 0f
        }
    }

    /**
     * Applies semi-implicit euler and drag forces.
     *
     * @param dt Timestep
     */
    private fun applyForces(dt: Float) {
        for (b in getRigidBodies()) {
            if (b !is PhysicalBodyInterface) continue
            if (b.invMass == 0f) {
                continue
            }
            applyLinearDrag(b)
            if (b.affectedByGravity) {
                b.velocity.add(gravity.scalar(dt))
            }
            b.velocity.add(b.force.scalar(b.invMass).scalar(dt))
            b.angularVelocity += dt * b.invInertia * b.torque
        }
    }

    /**
     * Method to apply all forces in the world.
     */
    private fun solve() {
        /*
        Resolve joints
        Note: this is removed from the iterations at this stage as the application of forces is different.
        The extra iterations on joints make the forces of the joints multiple times larger equal to the number of iterations.
        Early out could be used like in the collision solver
        This may change in the future and will be revised at a later date.
        */
        for (j in getJoints()) {
            j.applyTension()
        }

        //Resolve collisions
        for (i in 0 until Physics.ITERATIONS) {
            for (contact in contacts) {
                contact.solve()
            }
        }
    }

    /**
     * Applies linear drag to a body.
     *
     * @param b Body to apply drag to.
     */
    private fun applyLinearDrag(b: PhysicalBodyInterface?) {
        val velocityMagnitude = b!!.velocity.length()
        val dragForceMagnitude = velocityMagnitude * velocityMagnitude * b.linearDampening
        val dragForceVector = b.velocity.normalized.scalar(-dragForceMagnitude)
        b.applyForce(dragForceVector)
    }

    /**
     * A discrete Broad phase check of collision detection.
     */
    private fun broadPhaseCheck() {
        val bodies = getRigidBodies()
        for (i in bodies.indices) {
            val a = bodies[i]
            for (x in i + 1 until bodies.size) {
                val b = bodies[x]
                if (a !is CollisionBodyInterface || b !is CollisionBodyInterface) continue

                //Ignores static or particle objects
                if (a is PhysicalBodyInterface && b is PhysicalBodyInterface && (a.invMass == 0f && b.invMass == 0f || a.particle && b.particle)) {
                    continue
                }
                if (AxisAlignedBoundingBox.aabbOverlap(a, b)) {
                    narrowPhaseCheck(a, b)
                }
            }
        }
    }

    /**
     * If broad phase detection check passes, a narrow phase check is conducted to determine for certain if two objects are intersecting.
     * If two objects are, arbiters of contacts found are generated
     *
     * @param a
     * @param b
     */
    private fun narrowPhaseCheck(a: CollisionBodyInterface, b: CollisionBodyInterface) {
        val contactQuery = Arbiter(a, b)
        contactQuery.narrowPhase()
        if (contactQuery.contactCount > 0) {
            contacts.add(contactQuery)
        }
    }

    /**
     * Clears all objects in the current world
     */
    fun clearWorld() {
        // TODO bodies.clear()
        contacts.clear()
        // TODO: joints.clear()
    }

    /**
     * Applies gravitational forces between to objects (force applied to centre of body)
     */
    fun gravityBetweenObj() {
        val bodies = getRigidBodies()
        for (a in bodies.indices) {
            val bodyA = bodies[a]
            for (b in a + 1 until bodies.size) {
                val bodyB = bodies[b]
                if (bodyB !is PhysicalBodyInterface || bodyA !is PhysicalBodyInterface) continue
                val distance = bodyA.position.distance(bodyB.position)
                val force = 6.67f.pow(-11f) * bodyA.mass * bodyB.mass / (distance.raw * distance.raw)
                var direction: Vec2? =
                    Vec2(bodyB.position.x - bodyA.position.x, bodyB.position.y - bodyA.position.y)
                direction = direction!!.scalar(force)
                val oppositeDir = Vec2(-direction.x, -direction.y)
                bodyA.force.plus(direction)
                bodyB.force.plus(oppositeDir)
            }
        }
    }
}