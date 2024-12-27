package com.pandulapeter.kubriko.extensions

import com.pandulapeter.kubriko.types.AngleDegrees
import kotlin.math.PI

val AngleDegrees.rad get() = (normalized * (PI / 180f).toFloat()).rad