package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.implementation.extensions.cos
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Should be implemented by [Actor]s that want their positions to be updated automatically by [Kubriko] in function of their speed, direction and friction.
 * [Movable] [Actor]s must also be [Dynamic] and [Positionable].
 */
interface Movable : Dynamic, Positionable {

    /**
     * The speed of the Actor in [SceneUnit]-s / milliseconds.
     */
    var speed: SceneUnit

    /**
     * The friction applied to the speed in [SceneUnit]-s / milliseconds.
     */
    val friction: SceneUnit get() = SceneUnit.Zero

    /**
     * The direction the Actor should be moving towards.
     */
    val direction: AngleRadians get() = AngleRadians.Zero

    /**
     * Actors can overrides this function, but they should call the super implementation to take advantage of the full feature set of [Movable].
     */
    override fun update(deltaTimeInMillis: Float) {
        if (speed != SceneUnit.Zero) {
            speed -= friction * deltaTimeInMillis
            if (speed.raw < MINIMUM_SPEED) {
                speed = SceneUnit.Zero
            }
            body.position += SceneOffset(
                x = direction.cos.sceneUnit,
                y = -direction.sin.sceneUnit
            ) * speed * deltaTimeInMillis
        }
    }

    companion object {
        private const val MINIMUM_SPEED = 0.01f
    }
}