package com.pandulapeter.kubriko.engine.implementation.extensions

import com.pandulapeter.kubriko.engine.types.AngleDegrees
import kotlin.math.PI

fun Float.radiansToDegrees() = (this * 180f / PI.toFloat()).deg

val Float.deg: AngleDegrees
    get() = AngleDegrees(this)