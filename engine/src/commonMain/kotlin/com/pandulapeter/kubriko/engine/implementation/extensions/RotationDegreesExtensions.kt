package com.pandulapeter.kubriko.engine.implementation.extensions

import com.pandulapeter.kubriko.engine.types.AngleDegrees
import kotlin.math.PI

fun AngleDegrees.toRadians() = normalized * (PI / 180f).toFloat()