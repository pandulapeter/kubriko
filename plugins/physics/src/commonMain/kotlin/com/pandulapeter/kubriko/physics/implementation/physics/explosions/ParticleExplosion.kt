package com.pandulapeter.kubriko.physics.implementation.physics.explosions

import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.World
import com.pandulapeter.kubriko.physics.implementation.physics.math.Mat2
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Circle

/**
 * Models particle explosions.
 *
 * @param epicentre     Vector location of explosion epicenter.
 * @param noOfParticles Total number of particles the explosion has.
 * @param lifespan          The life time of the particle.
 */
class ParticleExplosion(private val epicentre: Vec2, private val noOfParticles: Int, private val lifespan: Double) {
    /**
     * Getter to return the list of particles in the world.
     *
     * @return Array of bodies.
     */
    val particles = MutableList(noOfParticles) { Body(Circle(.0), .0, .0) }

    /**
     * Creates particles in the supplied world.
     *
     * @param size    The size of the particles.
     * @param density The density of the particles.
     * @param radius  The distance away from the epicenter the particles are placed.
     * @param world   The world the particles are created in.
     */
    fun createParticles(size: Double, density: Int, radius: Int, world: World) {
        val separationAngle = 6.28319 / noOfParticles
        val distanceFromCentre = Vec2(.0, radius.toDouble())
        val rotate = Mat2(separationAngle)
        for (i in 0 until noOfParticles) {
            val particlePlacement = epicentre.plus(distanceFromCentre)
            val b = Body(Circle(size), particlePlacement.x, particlePlacement.y)
            b.density = density.toDouble()
            b.restitution = 1.0
            b.staticFriction = 0.0
            b.dynamicFriction = 0.0
            b.affectedByGravity = false
            b.linearDampening = 0.0
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
    fun applyBlastImpulse(blastPower: Double) {
        var line: Vec2
        for (b in particles) {
            line = b.position.minus(epicentre)
            b.velocity.set(line.scalar(blastPower))
        }
    }
}