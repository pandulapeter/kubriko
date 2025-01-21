package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

abstract class Particle<T : Particle<T>>(
    private val emitter: ParticleEmitter<*, T>,
    protected var speed: SceneUnit = SceneUnit.Zero,
    protected var direction: AngleRadians = AngleRadians.Zero,
) : Visible, Dynamic {
    private lateinit var particleManager: ParticleManagerImpl
    private lateinit var viewportManager: ViewportManager

    abstract fun updateParticle(deltaTimeInMilliseconds: Float)

    override fun onAdded(kubriko: Kubriko) {
        particleManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    protected fun removeAndCache() = particleManager.remove(emitter, this)

    final override fun update(deltaTimeInMilliseconds: Float) {
        if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
            removeAndCache()
        } else {
            if (speed != SceneUnit.Zero) {
                body.position = SceneOffset(
                    x = body.position.x + speed * direction.cos,
                    y = body.position.y - speed * direction.sin,
                )
            }
            updateParticle(deltaTimeInMilliseconds)
        }
    }
}