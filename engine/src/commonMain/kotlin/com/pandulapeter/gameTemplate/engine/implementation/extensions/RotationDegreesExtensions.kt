package com.pandulapeter.gameTemplate.engine.implementation.extensions

import com.pandulapeter.gameTemplate.engine.types.AngleDegrees
import kotlin.math.PI

fun AngleDegrees.toRadians() = normalized * (PI / 180f).toFloat()