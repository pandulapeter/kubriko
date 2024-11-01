package com.pandulapeter.kubriko.engine.traits

import com.pandulapeter.kubriko.engine.types.AngleRadians
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import kotlin.math.cos
import kotlin.math.sin

interface Movable : Dynamic, Visible {
    var speed: Float
    val friction: Float get() = 0f
    val direction: AngleRadians get() = AngleRadians.Zero

    override fun update(deltaTimeInMillis: Float) {
        if (speed != 0f) {
            speed -= friction * deltaTimeInMillis
            if (speed < 0.01f) {
                speed = 0f
            }
            position += WorldCoordinates(
                x = cos(direction.normalized),
                y = -sin(direction.normalized)
            ) * speed * deltaTimeInMillis
        }
    }
}