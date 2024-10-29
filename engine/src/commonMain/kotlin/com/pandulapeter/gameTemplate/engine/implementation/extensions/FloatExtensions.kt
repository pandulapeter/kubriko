package com.pandulapeter.gameTemplate.engine.implementation.extensions

import com.pandulapeter.gameTemplate.engine.types.AngleDegrees
import kotlin.math.PI

fun Float.radiansToDegrees() = (this * 180f / PI.toFloat()).deg

val Float.deg: AngleDegrees
    get() = AngleDegrees(this)