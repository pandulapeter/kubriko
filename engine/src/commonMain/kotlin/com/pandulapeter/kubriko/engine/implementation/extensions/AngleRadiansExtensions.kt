package com.pandulapeter.kubriko.engine.implementation.extensions

import com.pandulapeter.kubriko.engine.types.AngleRadians
import kotlin.math.PI

val AngleRadians.deg get() = normalized * (180f / PI).toFloat()