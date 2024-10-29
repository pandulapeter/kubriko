package com.pandulapeter.gameTemplate.engine.implementation.extensions

import com.pandulapeter.gameTemplate.engine.types.RotationDegrees
import kotlin.math.PI

fun RotationDegrees.toRadians() = normalized * (PI / 180f).toFloat()