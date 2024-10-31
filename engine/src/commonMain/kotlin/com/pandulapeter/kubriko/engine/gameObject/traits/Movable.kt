package com.pandulapeter.kubriko.engine.gameObject.traits

import com.pandulapeter.kubriko.engine.implementation.extensions.deg
import com.pandulapeter.kubriko.engine.implementation.extensions.toRadians
import com.pandulapeter.kubriko.engine.types.AngleDegrees
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import kotlin.math.cos
import kotlin.math.sin

interface Movable : Dynamic, Visible {
    var speed: Float
    val friction: Float get() = 0f
    val direction: AngleDegrees get() = 0f.deg

    override fun update(deltaTimeInMillis: Float) {
        if (speed != 0f) {
            speed -= friction * deltaTimeInMillis
            if (speed < 0.01f) {
                speed = 0f
            }
            direction.toRadians().let { angleRadians ->
                position += WorldCoordinates(
                    x = cos(angleRadians),
                    y = -sin(angleRadians)
                ) * speed * deltaTimeInMillis
            }
        }
    }
}