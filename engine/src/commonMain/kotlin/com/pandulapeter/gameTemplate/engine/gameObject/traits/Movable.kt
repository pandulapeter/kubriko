package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import kotlin.math.cos
import kotlin.math.sin

interface Movable : Dynamic, Visible {

    var speed: Float
    var directionDegrees: Float

    override fun update(deltaTimeMillis: Float) {
        if (speed > 0) {
            directionDegrees.toRadians().let { directionRadians ->
                position += Offset(
                    x = cos(directionRadians),
                    y = -sin(directionRadians)
                ) * speed
            }
        }
    }
}