package com.pandulapeter.kubriko.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val AngleRadians.deg get() = (normalized * (180f / PI).toFloat()).deg

val AngleRadians.sin get() = sin(normalized)

val AngleRadians.cos get() = cos(normalized)