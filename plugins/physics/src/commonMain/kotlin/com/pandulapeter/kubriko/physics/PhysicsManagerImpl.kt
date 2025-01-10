package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.extensions.rad
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.physics.implementation.collision.Arbiter
import com.pandulapeter.kubriko.physics.implementation.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physics.implementation.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physics.implementation.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physics.implementation.math.Vec2
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class PhysicsManagerImpl(
    initialGravity: SceneOffset,
    initialSimulationSpeed: Float,
    isLoggingEnabled: Boolean,
) : PhysicsManager(isLoggingEnabled) {
    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
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
    override val gravity = MutableStateFlow(initialGravity)
    private val actualGravity by autoInitializingLazy {
        gravity.map { Vec2(it.x, it.y) }.asStateFlow(Vec2(initialGravity.x, initialGravity.y))
    }
    override val simulationSpeed = MutableStateFlow(initialSimulationSpeed)

    override fun onUpdate(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) {
        if (stateManager.isRunning.value && deltaTimeInMilliseconds > 0) {
            arbiters.clear()
            broadPhaseCheck()
            semiImplicit(deltaTimeInMilliseconds * simulationSpeed.value / 100f)
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
                b.velocity.add(actualGravity.value.scalar(dt))
            }
            b.velocity.add(b.force.scalar(b.invMass).scalar(dt))
            b.angularVelocity += dt * b.invInertia * b.torque
        }
    }

    private fun solve() {
        for (joint in joints.value) {
            joint.applyTension()
        }
        for (arbiter in arbiters) {
            arbiter.solve()
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