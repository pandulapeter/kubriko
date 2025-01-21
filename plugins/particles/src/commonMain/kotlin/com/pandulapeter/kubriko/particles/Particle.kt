package com.pandulapeter.kubriko.particles

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.cos
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.extensions.sin
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

abstract class Particle(
    private val speed: SceneUnit = SceneUnit.Zero,
    private val direction: AngleRadians = AngleRadians.Zero,
) : Visible, Dynamic {
    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager

    abstract fun updateParticle(deltaTimeInMilliseconds: Float)

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    protected fun remove() = actorManager.remove(this)

    final override fun update(deltaTimeInMilliseconds: Float) {
        if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
            remove()
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