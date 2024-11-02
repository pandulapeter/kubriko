package com.pandulapeter.kubriko.traits

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Should be implemented by Actors that want their positions to be updated automatically by [Kubriko] in function of their speed, direction and friction.
 * [Movable] Actors must also be [Dynamic] and [Positionable].
 */
interface Movable : Dynamic, Positionable {

    /**
     * The speed of the Actor in [ScenePixel]-s / milliseconds.
     */
    var speed: ScenePixel

    /**
     * The friction applied to the speed in [ScenePixel]-s / milliseconds.
     */
    val friction: ScenePixel get() = ScenePixel.Zero

    /**
     * The direction the Actor should be moving towards.
     */
    val direction: AngleRadians get() = AngleRadians.Zero

    /**
     * Actors can overrides this function, but they should call the super implementation to take advantage of the full feature set of [Movable].
     */
    override fun update(deltaTimeInMillis: Float) {
        if (speed != ScenePixel.Zero) {
            speed -= friction * deltaTimeInMillis
            if (speed.raw < MINIMUM_SPEED) {
                speed = ScenePixel.Zero
            }
            position += SceneOffset(
                x = cos(direction.normalized).scenePixel,
                y = -sin(direction.normalized).scenePixel
            ) * speed * deltaTimeInMillis
        }
    }

    companion object {
        private const val MINIMUM_SPEED = 0.01f
    }
}