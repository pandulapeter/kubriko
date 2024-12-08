package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.physics.JointWrapper
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.collision.Arbiter
import com.pandulapeter.kubriko.physics.implementation.physics.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.physics.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Physics
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class PhysicsManagerImpl(
    gravity: SceneOffset,
) : PhysicsManager() {
    private val gravity = Vec2(gravity.x, gravity.y)
    private lateinit var actorManager: ActorManager
    private lateinit var stateManager: StateManager
    private val rigidBodies by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<RigidBody>().map { it.physicsBody } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    private val joints by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<JointWrapper>().map { it.physicsJoint } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    private val arbiters = mutableListOf<Arbiter>()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        stateManager = kubriko.require()
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        if (stateManager.isRunning.value && deltaTimeInMillis > 0) {
            arbiters.clear()
            broadPhaseCheck()
            semiImplicit(deltaTimeInMillis / 100f)
            for (contact in arbiters) {
                contact.penetrationResolution()
            }
        }
    }

    private fun semiImplicit(dt: Float) {
        applyForces(dt)
        solve()
        for (b in rigidBodies.value) {
            if (b.invMass == 0f) {
                continue
            }
            b.position.add(b.velocity.scalar(dt))
            b.orientation += (dt * b.angularVelocity).rad
            b.force.set(0f.sceneUnit, 0f.sceneUnit)
            b.torque = 0f
        }
    }

    private fun applyForces(dt: Float) {
        for (b in rigidBodies.value) {
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

    private fun solve() {
        for (j in joints.value) {
            j.applyTension()
        }
        for (i in 0 until Physics.ITERATIONS) {
            for (contact in arbiters) {
                contact.solve()
            }
        }
    }

    private fun applyLinearDrag(b: PhysicalBodyInterface?) {
        val velocityMagnitude = b!!.velocity.length()
        val dragForceMagnitude = velocityMagnitude * velocityMagnitude * b.linearDampening
        val dragForceVector = b.velocity.normalized.scalar(-dragForceMagnitude)
        b.applyForce(dragForceVector)
    }

    private fun broadPhaseCheck() {
        val bodies = rigidBodies.value
        for (i in bodies.indices) {
            val a = bodies[i]
            for (x in i + 1 until bodies.size) {
                val b = bodies[x]
                if (a.invMass == 0f && b.invMass == 0f || a.particle && b.particle) {
                    continue
                }
                if (AxisAlignedBoundingBox.aabbOverlap(a, b)) {
                    narrowPhaseCheck(a, b)
                }
            }
        }
    }

    private fun narrowPhaseCheck(a: CollisionBodyInterface, b: CollisionBodyInterface) {
        val contactQuery = Arbiter(a, b)
        contactQuery.narrowPhase()
        if (contactQuery.contactCount > 0) {
            arbiters.add(contactQuery)
        }
    }
}