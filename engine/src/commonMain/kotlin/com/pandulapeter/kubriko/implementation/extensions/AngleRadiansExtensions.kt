package com.pandulapeter.kubriko.implementation.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import kotlin.math.PI

val AngleRadians.deg get() = (normalized * (180f / PI).toFloat()).deg