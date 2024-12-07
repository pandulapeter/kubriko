package com.pandulapeter.kubriko.physics.implementation.physics.explosions

import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.World
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.physics.math.Mat2
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.PI

/**
 * Models particle explosions.
 *
 * @param epicentre     Vector location of explosion epicenter.
 * @param noOfParticles Total number of particles the explosion has.
 * @param lifespan          The life time of the particle.
 */
class ParticleExplosion(private val epicentre: Vec2, private val noOfParticles: Int, private val lifespan: Float) {
    /**
     * Getter to return the list of particles in the world.
     *
     * @return Array of bodies.
     */
    val particles = MutableList(noOfParticles) { Body(Circle(SceneUnit.Zero), SceneUnit.Zero, SceneUnit.Zero) }

    /**
     * Creates particles in the supplied world.
     *
     * @param size    The size of the particles.
     * @param density The density of the particles.
     * @param radius  The distance away from the epicenter the particles are placed.
     * @param world   The world the particles are created in.
     */
    fun createParticles(size: SceneUnit, density: Int, radius: SceneUnit, world: World) {
        val separationAngle = (PI.toFloat() * 2) / noOfParticles
        val distanceFromCentre = Vec2(0.sceneUnit, radius)
        val rotate = Mat2(separationAngle)
        for (i in 0 until noOfParticles) {
            val particlePlacement = epicentre.plus(distanceFromCentre)
            val b = Body(Circle(size), particlePlacement.x, particlePlacement.y)
            b.density = density.toFloat()
            b.restitution = 1f
            b.staticFriction = 0f
            b.dynamicFriction = 0f
            b.affectedByGravity = false
            b.linearDampening = 0f
            b.particle = true
            world.addBody(b)
            particles[i] = b
            rotate.mul(distanceFromCentre)
        }
    }

    /**
     * Applies a blast impulse to all particles created.
     *
     * @param blastPower The impulse magnitude.
     */
    fun applyBlastImpulse(blastPower: Float) {
        var line: Vec2
        for (b in particles) {
            line = b.position.minus(epicentre)
            b.velocity.set(line.scalar(blastPower))
        }
    }
}